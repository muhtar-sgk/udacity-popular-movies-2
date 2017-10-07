package com.b2uty.aamovies;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.b2uty.aamovies.adapter.MovieAdapter;
import com.b2uty.aamovies.data.MovieContract;
import com.b2uty.aamovies.models.Result;
import com.b2uty.aamovies.utilities.ImageUtils;
import com.b2uty.aamovies.utilities.JSONUtils;
import com.b2uty.aamovies.utilities.NetworkUtils;
import com.b2uty.aamovies.utilities.URLContent;
import com.facebook.stetho.Stetho;

import java.util.ArrayList;
import java.util.List;

public class MovieGridActivity extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener, MovieAdapter.GridItemClickListener {

    private static final String LOG_TAG = MovieGridActivity.class.getSimpleName();

    private static final int FETCH_MOVIE_FROM_INTERNET_ID = 11;
    private static final int FETCH_MOVIE_FROM_DB_ID = 21;
    private static final int MSG_LOAD_FIRST_ITEM = 61;
    private static final String DETAIL_FRAGMENT_TAG = "fragment_tag";

    private MovieAdapter mMovieAdapter;
    private Context mContext = MovieGridActivity.this;
    private Toast mToast;
    private Boolean mTwoPane = false;
    private TextView mEmptyView;
    private RecyclerView mRecyclerView;
    private ProgressBar mProgressBar;

    public static final String mProjection[] = {MovieContract.FavouriteEntry._ID, MovieContract.FavouriteEntry.COLUMN_TITLE, MovieContract.FavouriteEntry.COLUMN_MOVIE_ID, MovieContract.FavouriteEntry.COLUMN_RELEASE_DATE, MovieContract.FavouriteEntry.COLUMN_USER_RATING, MovieContract.FavouriteEntry.COLUMN_SYNOPSIS, MovieContract.FavouriteEntry.COLUMN_POSTER, MovieContract.FavouriteEntry.COLUMN_BACKDROP};

    public static final int INDEX_MOVIE_TITLE = 1;
    public static final int INDEX_MOVIE_ID = 2;
    public static final int INDEX_MOVIE_RELEASE_DATE = 3;
    public static final int INDEX_MOVIE_USER_RATING = 4;
    public static final int INDEX_MOVIE_SYNOPSIS = 5;
    public static final int INDEX_MOVIE_POSTER = 6;
    public static final int INDEX_MOVIE_BACKDROP = 7;
    public static final int INDEX_MOVIE_CAST = 7;

    private static Handler mHandler;

    private SharedPreferences mSharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Stetho.InitializerBuilder initializerBuilder = Stetho.newInitializerBuilder(this);
        initializerBuilder.enableWebKitInspector(Stetho.defaultInspectorModulesProvider(this));
        Stetho.Initializer initializer = initializerBuilder.build();
        Stetho.initialize(initializer);

        setContentView(R.layout.activity_movie_grid);

        if (findViewById(R.id.movie_detail_container) != null)
            mTwoPane = true;

        mEmptyView = (TextView) findViewById(R.id.tv_empty_view_main_ui);
        mRecyclerView = (RecyclerView) findViewById(R.id.rv_main_ui);
        mProgressBar = (ProgressBar) findViewById(R.id.pb_main_ui);

        RecyclerView.LayoutManager mLayoutManager;
        if (mTwoPane)
            mLayoutManager = new GridLayoutManager(mContext, 2);
        else
            mLayoutManager = new GridLayoutManager(mContext, ImageUtils.calculateNoOfColumns(mContext));

        mRecyclerView.setLayoutManager(mLayoutManager);
        mMovieAdapter = new MovieAdapter(MovieGridActivity.this, this);
        mRecyclerView.setAdapter(mMovieAdapter);

        mSharedPreferences = getPreferences(Context.MODE_PRIVATE);

        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MSG_LOAD_FIRST_ITEM) {
                    if (mMovieAdapter.getItemCount() > 0)
                        onClick(mMovieAdapter.getFirstMovieData());
                    else
                        displayBlankRightPane();
                }
            }
        };

        if (savedInstanceState == null) {
            if (mSharedPreferences.getString(getString(R.string.sort_by), getString(R.string.most_popular)).equals(getString(R.string.favourite))) {
                showFetchingDataUI();
                getSupportLoaderManager().initLoader(FETCH_MOVIE_FROM_DB_ID, null, new CursorCallback());
            } else {
                if (!NetworkUtils.isConnectedToInternet(mContext))
                    showNoInternetUI();
                else {
                    showFetchingDataUI();
                    getSupportLoaderManager().initLoader(FETCH_MOVIE_FROM_INTERNET_ID, null, new ResultCallback());
                }
            }
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (mSharedPreferences.getString(getString(R.string.sort_by), getString(R.string.most_popular)).equals(getString(R.string.favourite))) {
            showFetchingDataUI();
            getSupportLoaderManager().initLoader(FETCH_MOVIE_FROM_DB_ID, null, new CursorCallback());
        } else {
            if (!NetworkUtils.isConnectedToInternet(mContext)) {
                showNoInternetUI();
                displayBlankRightPane();
            } else {
                showFetchingDataUI();
                getSupportLoaderManager().initLoader(FETCH_MOVIE_FROM_INTERNET_ID, null, new ResultCallback());
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_movie_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.i_sort_by:
                showMenu(findViewById(R.id.i_sort_by));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        switch (item.getItemId()) {
            case R.id.i_most_popular:
                showToast(getString(R.string.most_popular));
                editor.putString(getString(R.string.sort_by), getString(R.string.most_popular));
                editor.apply();
                if (!NetworkUtils.isConnectedToInternet(mContext)) {
                    showNoInternetUI();
                    if (mTwoPane)
                        displayBlankRightPane();
                } else {
                    showFetchingDataUI();
                    getSupportLoaderManager().restartLoader(FETCH_MOVIE_FROM_INTERNET_ID, null, new ResultCallback());
                }
                return true;
            case R.id.i_highest_rated:
                showToast(getString(R.string.highest_rated));
                editor.putString(getString(R.string.sort_by), getString(R.string.highest_rated));
                editor.commit();
                if (!NetworkUtils.isConnectedToInternet(mContext)) {
                    showNoInternetUI();
                    if (mTwoPane)
                        displayBlankRightPane();
                } else {
                    showFetchingDataUI();
                    getSupportLoaderManager().restartLoader(FETCH_MOVIE_FROM_INTERNET_ID, null, new ResultCallback());
                }
                return true;

            case R.id.i_upcoming:
                showToast(getString(R.string.upcoming));
                editor.putString(getString(R.string.sort_by), getString(R.string.upcoming));
                editor.commit();
                if (!NetworkUtils.isConnectedToInternet(mContext)) {
                    showNoInternetUI();
                    if (mTwoPane)
                        displayBlankRightPane();
                } else {
                    showFetchingDataUI();
                    getSupportLoaderManager().restartLoader(FETCH_MOVIE_FROM_INTERNET_ID, null, new ResultCallback());
                }
                return true;

            case R.id.i_now_playing:
                showToast(getString(R.string.now_playing));
                editor.putString(getString(R.string.sort_by), getString(R.string.now_playing));
                editor.commit();
                if (!NetworkUtils.isConnectedToInternet(mContext)) {
                    showNoInternetUI();
                    if (mTwoPane)
                        displayBlankRightPane();
                } else {
                    showFetchingDataUI();
                    getSupportLoaderManager().restartLoader(FETCH_MOVIE_FROM_INTERNET_ID, null, new ResultCallback());
                }
                return true;
            case R.id.i_favourite:
                showToast(getString(R.string.favourite));
                editor.putString(getString(R.string.sort_by), getString(R.string.favourite));
                editor.commit();
                showFetchingDataUI();
                getSupportLoaderManager().restartLoader(FETCH_MOVIE_FROM_DB_ID, null, new CursorCallback());
                return true;
            default:
                return false;
        }
    }

    public void showMenu(View v) {
        PopupMenu popup = new PopupMenu(MovieGridActivity.this, v);

        popup.setOnMenuItemClickListener(this);
        popup.inflate(R.menu.sortby_menu);
        popup.show();
    }

    public void showToast(String message) {
        if (mToast != null)
            mToast.cancel();
        mToast = Toast.makeText(mContext, "Show " + message + " movies", Toast.LENGTH_SHORT);
        mToast.show();
    }

    public void showNoInternetUI() {
        mProgressBar.setVisibility(View.GONE);
        mMovieAdapter.setMovieData(null);
        mEmptyView.setText(getString(R.string.no_internet));
    }

    public void showFetchingDataUI() {
        mProgressBar.setVisibility(View.VISIBLE);
        mMovieAdapter.setMovieData(null);
        mEmptyView.setText("");
    }

    public void showNoDataUI() {
        mProgressBar.setVisibility(View.GONE);
        mMovieAdapter.setMovieData(null);
        mEmptyView.setText(getString(R.string.no_data_fetched));
    }

    private void displayBlankRightPane() {
        MovieDetailFragment movieDetailFragment = (MovieDetailFragment) getSupportFragmentManager()
                .findFragmentByTag(DETAIL_FRAGMENT_TAG);
        if (movieDetailFragment != null)
            getSupportFragmentManager().beginTransaction().remove(movieDetailFragment).commit();
    }

    private class ResultCallback implements LoaderManager.LoaderCallbacks<List<Result>> {
        @Override
        public Loader<List<Result>> onCreateLoader(int id, final Bundle args) {
            return new AsyncTaskLoader<List<Result>>(MovieGridActivity.this) {

                private List<Result> mMovieList;

                @Override
                protected void onStartLoading() {
                    if (mMovieList != null)
                        deliverResult(mMovieList);
                    else
                        forceLoad();
                }

                @Override
                public void deliverResult(List<Result> data) {
                    mMovieList = data;
                    super.deliverResult(data);
                }

                @Override
                public List<Result> loadInBackground() {
                    String choice = mSharedPreferences.getString(getString(R.string.sort_by), getString(R.string.most_popular));
                    String finalUrl = URLContent.getInitialUrl(MovieGridActivity.this, choice);
                    String movieListJsonResponse = NetworkUtils.makeHTTPRequest(finalUrl);
                    return JSONUtils.parseMovieListJSON(movieListJsonResponse);
                }
            };
        }

        @Override
        public void onLoadFinished(Loader<List<Result>> loader, List<Result> data) {
            if (data != null && !data.isEmpty()) {
                mProgressBar.setVisibility(View.GONE);
                mMovieAdapter.setMovieData(data);
            } else {
                mMovieAdapter.setMovieData(null);
                if (!NetworkUtils.isConnectedToInternet(mContext))
                    showNoInternetUI();
                else
                    showNoDataUI();
            }
            if (mTwoPane)
                mHandler.sendEmptyMessage(MSG_LOAD_FIRST_ITEM);
        }

        @Override
        public void onLoaderReset(Loader<List<Result>> loader) {

        }
    }

    private class CursorCallback implements LoaderManager.LoaderCallbacks<Cursor> {

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            switch (id) {
                case FETCH_MOVIE_FROM_DB_ID:
                    return new CursorLoader(mContext, MovieContract.FavouriteEntry.CONTENT_URI, mProjection, null, null, null);
                default:
                    return null;
            }
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            if (!mSharedPreferences.getString(getString(R.string.sort_by), getString(R.string.most_popular)).equals(getString(R.string.favourite)))
                return;
            List<Result> movieList = new ArrayList<>();
            if (data.moveToFirst()) {
                do {
                    int id = data.getInt(INDEX_MOVIE_ID);
                    String title = data.getString(INDEX_MOVIE_TITLE);
                    String synopsis = data.getString(INDEX_MOVIE_SYNOPSIS);
                    String releaseDate = data.getString(INDEX_MOVIE_RELEASE_DATE);
                    Double userRating = data.getDouble(INDEX_MOVIE_USER_RATING);
                    byte[] posterImage = data.getBlob(INDEX_MOVIE_POSTER);
                    byte[] backdropImage = data.getBlob(INDEX_MOVIE_BACKDROP);

                    Result movie = new Result(id, title, synopsis, releaseDate, userRating, posterImage, backdropImage);
                    movieList.add(movie);
                } while (data.moveToNext());
            }
            mProgressBar.setVisibility(View.GONE);
            if (!movieList.isEmpty()) {
                mProgressBar.setVisibility(View.GONE);
                mMovieAdapter.setMovieData(movieList);
            } else {
                mMovieAdapter.setMovieData(null);
                showNoDataUI();
            }
            if (mTwoPane)
                mHandler.sendEmptyMessage(MSG_LOAD_FIRST_ITEM);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {

        }
    }

    @Override
    public void onClick(Result object) {
        if (mTwoPane) {
            Bundle bundle = new Bundle();
            bundle.putSerializable(MovieDetailFragment.MOVIE_ARG_KEY, object);
            MovieDetailFragment movieDetailFragment = new MovieDetailFragment();
            movieDetailFragment.setArguments(bundle);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.movie_detail_container, movieDetailFragment, DETAIL_FRAGMENT_TAG).commit();
        } else {
            Intent detailIntent = new Intent(MovieGridActivity.this, MovieDetailActivity.class);
            detailIntent.putExtra(MovieDetailFragment.MOVIE_ARG_KEY, object);
            startActivity(detailIntent);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}