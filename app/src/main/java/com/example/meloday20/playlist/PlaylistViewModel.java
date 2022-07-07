package com.example.meloday20.playlist;

import android.app.Application;
import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.meloday20.MeloDayPlaylistTrack;
import com.example.meloday20.MeloDayPlaylistTrackDao;
import com.example.meloday20.MyDatabaseApplication;
import com.example.meloday20.utils.ParseApplication;
import com.example.meloday20.utils.SpotifyServiceSingleton;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Pager;
import kaaes.spotify.webapi.android.models.Playlist;
import kaaes.spotify.webapi.android.models.PlaylistTrack;
import kaaes.spotify.webapi.android.models.UserPrivate;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class PlaylistViewModel extends AndroidViewModel {
    private static final String TAG = PlaylistViewModel.class.getSimpleName();
    private static String accessToken = ParseUser.getCurrentUser().getString("accessToken");
    public static SpotifyService spotify = SpotifyServiceSingleton.getInstance(accessToken);
    private ParseUser currentUser = ParseUser.getCurrentUser();
    private String userId = currentUser.getUsername();
    private MutableLiveData<Boolean> _playlistExists = new MutableLiveData<>();
    LiveData<Boolean> playlistExists = _playlistExists;
    private String playlistId;
    private MutableLiveData<List<MeloDayPlaylistTrack>> _playlistTracks = new MutableLiveData<>();
    LiveData<List<MeloDayPlaylistTrack>> playlistTracks = _playlistTracks;
    private MutableLiveData<Playlist> _playlistDetails = new MutableLiveData<>();
    LiveData<Playlist> playlistDetails = _playlistDetails;
    private MutableLiveData<UserPrivate> _userDetails = new MutableLiveData<>();
    LiveData<UserPrivate> userDetails = _userDetails;
    private Map<String, Object> createPlaylistParams = new HashMap<>();
    private String displayName;
    MeloDayPlaylistTrackDao meloDayPlaylistTrackDao = ((MyDatabaseApplication) getApplication().getApplicationContext()).getMyDatabase().meloDayPlaylistTrackDao();


    public PlaylistViewModel(@NonNull Application application) {
        super(application);
        getParsePlaylistId();
        getUserDetails();
    }

    public void getParsePlaylistId() {
        ParseQuery<ParsePlaylist> query = ParseQuery.getQuery(ParsePlaylist.class); // specify what type of data we want to query - ParsePlaylist.class
        query.whereEqualTo(ParsePlaylist.KEY_USER, currentUser);
        query.include(ParsePlaylist.KEY_PLAYLIST_ID); // include data referred by current user
        try {
            playlistId = query.find().get(0).getPlaylistId();
            _playlistExists.setValue(true);
        } catch (Exception e) {
            e.printStackTrace();
            _playlistExists.setValue(false);
        }
    }

//    public void getPlaylistTracksFromDB() {
//        AsyncTask.execute(new Runnable() {
//            @Override
//            public void run() {
//            });
//        };
//    }


    public void getPlaylistTracksFromNetworkAndSaveInDb() {
        spotify.getPlaylistTracks(userId, playlistId, new Callback<Pager<PlaylistTrack>>() {
            @Override
            public void success(Pager<PlaylistTrack> playlistTrackPager, Response response) {
                List<MeloDayPlaylistTrack> playlistTracksFromNetwork = new ArrayList<>();
                for (PlaylistTrack track : playlistTrackPager.items) {
                    try {
                        playlistTracksFromNetwork.add(new MeloDayPlaylistTrack().fromTrack(track));
                    } catch (java.text.ParseException e) {
                        e.printStackTrace();
                    }
                }

                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        Log.i(TAG, "Saving playlist tracks data into db");
                        meloDayPlaylistTrackDao.insertMeloDayPlaylistTrack(playlistTracksFromNetwork.toArray(new MeloDayPlaylistTrack[0]));
                    }
                });
//                _playlistTracks.setValue(meloDayPlaylistTracks);
//                _playlistTracks.setValue(playlistTrackPager.items);
            }
            @Override
            public void failure(RetrofitError error) {
                Log.e(TAG, "Error getting track details.", error);
            }
        });
    }

    public void getPlaylistDetails() {
        spotify.getPlaylist(userId, playlistId, new Callback<Playlist>() {
                @Override
                public void success(Playlist playlist, Response response) {
                    _playlistDetails.setValue(playlist);
                }
                @Override
                public void failure(RetrofitError error) {
                    Log.e(TAG, "Could not retrieve playlist from Spotify.", error);
                }
            });
    }

    public void getUserDetails() {
        spotify.getMe(new Callback<UserPrivate>() {
            @Override
            public void success(UserPrivate userPrivate, Response response) {
                displayName = userPrivate.display_name;
                _userDetails.setValue(userPrivate);
            }
            @Override
            public void failure(RetrofitError error) {
                Log.d(TAG, error.toString());
            }
        });
    }

    public void createNewPlaylist() {
        createPlaylistParams.put("name", displayName + "'s MeloDay");
        createPlaylistParams.put("description", "Your playlist of the year");
        createPlaylistParams.put("public", true);
        spotify.createPlaylist(userId, createPlaylistParams, new Callback<Playlist>() {
            @Override
            public void success(Playlist playlist, Response response) {
                Log.d(TAG, playlist.id);
                playlistId = playlist.id;
                savePlaylistIdInParse();
            }
            @Override
            public void failure(RetrofitError error) {
                Log.d(TAG, error.toString());
            }
        });
    }

    private void savePlaylistIdInParse() {
        ParsePlaylist playlist = new ParsePlaylist();
        playlist.setUser(currentUser);
        playlist.setPlaylistId(playlistId);
        playlist.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e != null) {
                    Log.e(TAG, "Parse Error while saving playlistId", e);
                }}
        });
    }

    public List<MeloDayPlaylistTrack> getMeloDayPlaylistTracksFromDb() {
       return meloDayPlaylistTrackDao.getAll();
    }
//    private void uploadPlaylistCover() throws JSONException {
//        JSONObject jsonBodyObj = new JSONObject();
//        try {
//            jsonBodyObj.put("playlist_id", playlistId);
//            jsonBodyObj.put("image", playlistId);
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//
//        final String requestBody = jsonBodyObj.toString();
//    }
}
