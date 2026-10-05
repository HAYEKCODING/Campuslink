package com.campuslink.realtime.service;

import com.campuslink.realtime.dto.response.LikeResponse;

import java.util.List;

public interface LikeService {

    LikeResponse liker(Long emetteurId, Long cibleId);

    void retirerLike(Long emetteurId, Long cibleId);

    List<LikeResponse> listerMesLikes(Long emetteurId);

    List<LikeResponse> listerLikesRecus(Long userId);
}
