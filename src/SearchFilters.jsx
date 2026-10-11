import React, { useState } from "react";
import { Link } from "react-router-dom";
import { Heart, SlidersHorizontal } from "lucide-react";
import Layout from "./Layout";
import { useAsyncData } from "./hooks/useAsyncData";
import { searchProfiles } from "./services/searchService";
import { getUniversities, getNeighborhoods, getInterests } from "./services/profileService";
import { GENDERS } from "./lib/referenceData";
import { LoadingState, EmptyState, ErrorState } from "./components/ui/AsyncStates";

/**
 * CampusLink — Recherche & Filtres
 *
 * Filtres alignés sur GET /profiles/search (backend) :
 * university, neighborhood, minAge/maxAge, interests (« au moins un »), genre.
 * Les listes servent de suggestions : les champs restent éditables, la
 * recherche backend étant une correspondance partielle.
 * Le genre est un filtre d'égalité stricte sur l'enum Gender (vide = tous).
 */

function SuggestInput({ label, placeholder, suggestions = [], value, onChange, listId }) {
  return (
    <div>
      <label htmlFor={listId} className="text-sm font-semibold text-slate-700 mb-1.5 block">
        {label}
      </label>
      <input
        id={listId}
        list={listId}
        type="text"
        placeholder={placeholder}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className="w-full px-4 py-2.5 text-sm rounded-lg border border-slate-200 text-slate-700 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-violet-400 focus:border-transparent"
      />
      <datalist id={listId}>
        {suggestions.map((opt) => (
          <option key={opt.id ?? opt} value={opt.id ?? opt}>
            {opt.label ?? opt}
          </option>
        ))}
      </datalist>
    </div>
  );
}

function AgeRange({ min, max, onChange }) {
  return (
    <div>
      <div className="flex items-center justify-between mb-1.5">
        <label className="text-sm font-semibold text-slate-700">Âge</label>
        <span className="text-xs text-slate-400">
          {min} — {max}
        </span>
      </div>
      <div className="relative h-6 flex items-center">
        <div className="absolute w-full h-1 bg-slate-100 rounded-full" />
        <div
          className="absolute h-1 bg-violet-500 rounded-full"
          style={{
            left: `${((min - 18) / (60 - 18)) * 100}%`,
            right: `${100 - ((max - 18) / (60 - 18)) * 100}%`,
          }}
        />
        <input
          type="range"
          min={18}
          max={60}
          value={min}
          onChange={(e) => onChange(Math.min(Number(e.target.value), max - 1), max)}
          aria-label="Âge minimum"
          className="absolute w-full appearance-none bg-transparent pointer-events-none [&::-webkit-slider-thumb]:pointer-events-auto [&::-webkit-slider-thumb]:appearance-none [&::-webkit-slider-thumb]:w-4 [&::-webkit-slider-thumb]:h-4 [&::-webkit-slider-thumb]:rounded-full [&::-webkit-slider-thumb]:bg-violet-600 [&::-webkit-slider-thumb]:cursor-pointer"
        />
        <input
          type="range"
          min={18}
          max={60}
          value={max}
          onChange={(e) => onChange(min, Math.max(Number(e.target.value), min + 1))}
          aria-label="Âge maximum"
          className="absolute w-full appearance-none bg-transparent pointer-events-none [&::-webkit-slider-thumb]:pointer-events-auto [&::-webkit-slider-thumb]:appearance-none [&::-webkit-slider-thumb]:w-4 [&::-webkit-slider-thumb]:h-4 [&::-webkit-slider-thumb]:rounded-full [&::-webkit-slider-thumb]:bg-violet-600 [&::-webkit-slider-thumb]:cursor-pointer"
        />
      </div>
    </div>
  );
}

function ResultRow({ id, name, age, university, faculty, photo }) {
  return (
    <Link to={`/app/profil/${id}`} className="flex items-center gap-3 p-3 rounded-xl hover:bg-slate-50 transition-colors">
      <div className="w-12 h-12 rounded-full bg-slate-100 flex-shrink-0 overflow-hidden flex items-center justify-center text-sm font-bold text-slate-400">
        {photo ? (
          <img src={photo} alt="" className="w-full h-full object-cover" />
        ) : (
          (name || "?").charAt(0)
        )}
      </div>
      <div className="flex-1 min-w-0">
        <p className="text-sm font-bold text-slate-900">
          {name}
          {age ? `, ${age}` : ""}
        </p>
        {university && <p className="text-xs text-slate-400 truncate">{university}</p>}
        {faculty && <p className="text-xs text-emerald-500 font-medium">{faculty}</p>}
      </div>
      <span className="w-8 h-8 rounded-full bg-slate-50 flex items-center justify-center flex-shrink-0">
        <Heart className="w-4 h-4 text-fuchsia-500" strokeWidth={1.75} />
      </span>
    </Link>
  );
}

export default function SearchFilters() {
  const [university, setUniversity] = useState("");
  const [neighborhood, setNeighborhood] = useState("");
  const [interest, setInterest] = useState("");
  const [gender, setGender] = useState("");
  const [ageRange, setAgeRange] = useState([18, 30]);
  const [results, setResults] = useState(null);
  const [searching, setSearching] = useState(false);
  const [searchError, setSearchError] = useState(null);

  const { data: universities } = useAsyncData(() => getUniversities(), []);
  const { data: neighborhoods } = useAsyncData(() => getNeighborhoods(), []);
  const { data: interests } = useAsyncData(() => getInterests(), []);

  const handleSearch = () => {
    setSearching(true);
    setSearchError(null);
    searchProfiles({
      university: university.trim(),
      neighborhood: neighborhood.trim(),
      interests: interest ? [interest] : [],
      gender,
      minAge: ageRange[0],
      maxAge: ageRange[1],
    })
      .then(setResults)
      .catch((err) => setSearchError(err.message))
      .finally(() => setSearching(false));
  };

  return (
    <Layout active="Recherche" showSearch={false}>
      <div className="max-w-4xl mx-auto grid md:grid-cols-[280px_1fr] gap-6">
        <div className="bg-white rounded-2xl shadow-sm p-6 h-fit">
          <div className="flex items-center justify-between mb-5">
            <h1 className="text-lg font-extrabold">Recherche</h1>
            <SlidersHorizontal className="w-4 h-4 text-slate-400" />
          </div>

          <p className="text-sm font-bold text-slate-500 mb-4">Filtres</p>

          <div className="space-y-5">
            <SuggestInput
              label="Université"
              listId="filtre-universite"
              placeholder="Toutes les universités"
              suggestions={universities || []}
              value={university}
              onChange={setUniversity}
            />

            <AgeRange
              min={ageRange[0]}
              max={ageRange[1]}
              onChange={(min, max) => setAgeRange([min, max])}
            />

            <div>
              <label htmlFor="filtre-genre" className="text-sm font-semibold text-slate-700 mb-1.5 block">
                Genre
              </label>
              <select
                id="filtre-genre"
                value={gender}
                onChange={(e) => setGender(e.target.value)}
                className="w-full px-4 py-2.5 text-sm rounded-lg border border-slate-200 text-slate-700 focus:outline-none focus:ring-2 focus:ring-violet-400 focus:border-transparent"
              >
                <option value="">Tous les genres</option>
                {GENDERS.map((opt) => (
                  <option key={opt.value} value={opt.value}>
                    {opt.label}
                  </option>
                ))}
              </select>
            </div>

            <SuggestInput
              label="Quartier"
              listId="filtre-quartier"
              placeholder="Tous les quartiers"
              suggestions={neighborhoods || []}
              value={neighborhood}
              onChange={setNeighborhood}
            />

            <SuggestInput
              label="Centres d'intérêt"
              listId="filtre-interet"
              placeholder="Tous les centres d'intérêt"
              suggestions={interests || []}
              value={interest}
              onChange={setInterest}
            />

            <button
              onClick={handleSearch}
              disabled={searching}
              className="w-full bg-gradient-to-r from-violet-600 to-fuchsia-500 hover:from-violet-700 hover:to-fuchsia-600 transition-colors text-white text-sm font-semibold py-3 rounded-lg disabled:opacity-60"
            >
              {searching ? "Recherche..." : "Rechercher"}
            </button>
          </div>
        </div>

        <div className="bg-white rounded-2xl shadow-sm p-6">
          <p className="text-sm font-bold text-slate-500 mb-2">Résultats</p>

          {searching && <LoadingState label="Recherche en cours..." />}
          {!searching && searchError && (
            <ErrorState label="La recherche a échoué." onRetry={handleSearch} />
          )}
          {!searching && !searchError && results === null && (
            <EmptyState label="Lancez une recherche pour voir des résultats." />
          )}
          {!searching && !searchError && results && results.length === 0 && (
            <EmptyState label="Aucun profil ne correspond à ces critères." />
          )}
          {!searching && !searchError && results && results.length > 0 && (
            <div className="divide-y divide-slate-50">
              {results.map((result) => (
                <ResultRow key={result.id} {...result} />
              ))}
            </div>
          )}
        </div>
      </div>
    </Layout>
  );
}
