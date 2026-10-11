/**
 * Validation et compression d'images côté client (upload avatar / photos).
 *
 * Le backend applique des règles strictes (voir application.media.*) :
 *   - types MIME acceptés : image/jpeg, image/png, image/webp
 *   - taille maximale : 5 Mo
 *   - dimension maximale : 1080 px (redimensionnée côté serveur)
 *
 * Sans cette étape, une photo de téléphone (souvent 8-15 Mo, parfois HEIC)
 * faisait échouer POST /media/upload avec un 400 et l'utilisateur bloquait
 * sur l'onboarding sans comprendre pourquoi. On prépare donc le fichier ici :
 * décodage via canvas, redimensionnement à 1080 px et ré-encodage JPEG avec
 * repli progressif de la qualité jusqu'à passer sous la limite de taille.
 *
 * Un fichier déjà conforme est renvoyé tel quel (aucune ré-encodage inutile).
 */

export const MAX_IMAGE_DIMENSION = 1080;
export const MAX_IMAGE_BYTES = 5 * 1024 * 1024; // 5 Mo (limite backend)
export const ACCEPTED_IMAGE_TYPES = ["image/jpeg", "image/png", "image/webp"];

/**
 * Contrôle le type MIME du fichier.
 * @returns {string|null} message d'erreur lisible, ou null si le type est accepté.
 */
export function validateImageType(file) {
  if (!file) return "Aucun fichier sélectionné.";
  if (!ACCEPTED_IMAGE_TYPES.includes(file.type)) {
    return `Format non supporté (${file.type || "inconnu"}). Utilisez une image JPG, PNG ou WebP.`;
  }
  return null;
}

/**
 * Prépare un fichier image pour l'upload : valide le type puis, si nécessaire,
 * redimensionne et compresse jusqu'à respecter la limite de taille backend.
 *
 * @param {File} file fichier original (JPG, PNG ou WebP)
 * @returns {Promise<File>} fichier prêt à envoyer (jamais > 5 Mo / 1080 px)
 * @throws {Error} message lisible si le fichier est illisible (ex. HEIC,
 *         image corrompue) — à afficher tel quel à l'utilisateur.
 */
export async function prepareImageForUpload(file) {
  const typeError = validateImageType(file);
  if (typeError) throw new Error(typeError);

  // Petit fichier JPEG/PNG/WebP déjà conforme : aucun ré-encodage.
  if (file.size <= MAX_IMAGE_BYTES) {
    const bitmap = await decodeImage(file).catch(() => null);
    if (!bitmap) {
      throw new Error("Impossible de lire cette image. Convertissez-la en JPG ou PNG et réessayez.");
    }
    const isSmallEnough =
      bitmap.width <= MAX_IMAGE_DIMENSION && bitmap.height <= MAX_IMAGE_DIMENSION;
    closeBitmap(bitmap);
    if (isSmallEnough) return file;
  }

  const bitmap = await decodeImage(file).catch(() => null);
  if (!bitmap) {
    throw new Error("Impossible de lire cette image. Convertissez-la en JPG ou PNG et réessayez.");
  }

  try {
    const scale = Math.min(1, MAX_IMAGE_DIMENSION / Math.max(bitmap.width, bitmap.height));
    const width = Math.max(1, Math.round(bitmap.width * scale));
    const height = Math.max(1, Math.round(bitmap.height * scale));

    const canvas = document.createElement("canvas");
    canvas.width = width;
    canvas.height = height;
    const ctx = canvas.getContext("2d");
    // Fond blanc : évite les fonds noirs sur les PNG transparents encodés en JPEG.
    ctx.fillStyle = "#ffffff";
    ctx.fillRect(0, 0, width, height);
    ctx.drawImage(bitmap, 0, 0, width, height);

    // Repli de qualité jusqu'à passer sous la limite de taille.
    for (const quality of [0.85, 0.7, 0.55, 0.4]) {
      const blob = await canvasToBlob(canvas, quality);
      if (blob && blob.size <= MAX_IMAGE_BYTES) {
        const baseName = (file.name || "photo").replace(/\.[^.]+$/, "");
        return new File([blob], `${baseName}.jpg`, {
          type: "image/jpeg",
          lastModified: Date.now(),
        });
      }
    }
    throw new Error("Image trop lourde même après compression. Essayez une photo plus petite.");
  } finally {
    closeBitmap(bitmap);
  }
}

/** Décodage via createImageBitmap, avec repli <img>+objectURL pour Safari <15. */
async function decodeImage(file) {
  if (typeof createImageBitmap === "function") {
    return createImageBitmap(file);
  }
  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(file);
    const img = new Image();
    img.onload = () => {
      URL.revokeObjectURL(url);
      resolve(img);
    };
    img.onerror = () => {
      URL.revokeObjectURL(url);
      reject(new Error("Décodage impossible"));
    };
    img.src = url;
  });
}

function closeBitmap(bitmap) {
  if (bitmap && typeof bitmap.close === "function") bitmap.close();
}

function canvasToBlob(canvas, quality) {
  return new Promise((resolve) => canvas.toBlob(resolve, "image/jpeg", quality));
}
