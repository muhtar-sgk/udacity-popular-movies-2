package com.b2uty.aamovies.utilities;

import com.b2uty.aamovies.models.MovieListResponse;
import com.b2uty.aamovies.models.MovieReviewResponse;
import com.b2uty.aamovies.models.MovieTrailerResponse;
import com.b2uty.aamovies.models.Result;
import com.b2uty.aamovies.models.Review;
import com.b2uty.aamovies.models.Trailer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.List;

/**
 * Created by Muhtar on 24/7/2017.
 */

public class JSONUtils {

    public static final String LOG_TAG = JSONUtils.class.getSimpleName();

    public static List<Result> parseMovieListJSON(String moviewListJsonResponse) {
        Gson gson = new GsonBuilder().create();
        if (moviewListJsonResponse == null)
            return null;
        MovieListResponse movieListResponse = gson.fromJson(moviewListJsonResponse, MovieListResponse.class);
        return movieListResponse.getResults();
    }

    public static List<Review> parseReviewJSON(String reviewJsonResponse) {
        Gson gson = new GsonBuilder().create();
        if (reviewJsonResponse == null)
            return null;
        MovieReviewResponse reviewListResponse = gson.fromJson(reviewJsonResponse, MovieReviewResponse.class);
        return reviewListResponse.getReviews();
    }

    public static List<Trailer> parseTrailerJSON(String trailerJsonResponse) {
        Gson gson = new GsonBuilder().create();
        if (trailerJsonResponse == null)
            return null;
        MovieTrailerResponse trailerListResponse = gson.fromJson(trailerJsonResponse, MovieTrailerResponse.class);
        return trailerListResponse.getTrailers();
    }
}