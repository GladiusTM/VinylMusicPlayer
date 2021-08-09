package com.poupa.vinylmusicplayer.provider;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.poupa.vinylmusicplayer.App;
import com.poupa.vinylmusicplayer.discog.Discography;
import com.poupa.vinylmusicplayer.model.Song;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Ordered list of songs, backed by a persistent storage
 * Can be used for playlist, play queue
 *
 * @author SC (soncaokim)
 */
abstract class SongList {
     String name;
     final List<Long> songIds = new ArrayList<>();

     SongList(@NonNull String name) {
          this.name = name;
          load();
     }

     abstract boolean load();
     abstract void save(@Nullable String newName);

     @NonNull
     ArrayList<Song> asSongs() {
          ArrayList<Song> result = new ArrayList<>();
          ArrayList<Long> orphanIds = new ArrayList<>();

          // TODO Do this cleaning and mapping once
          final Map<Long, Song> availableSongsById = Discography.getInstance().getAllSongsById();
          for (Long id : songIds) {
               final Song matchingSong = availableSongsById.get(id);
               if (matchingSong != null) {
                    result.add(matchingSong);
               } else {
                    orphanIds.add(id);
               }
          }

          if (!orphanIds.isEmpty()) {
               songIds.removeAll(orphanIds);
               save(null);
          }
          return result;
     }
}

abstract class MutableSongList extends SongList {
     MutableSongList(@NonNull String name) {
          super(name);
     }

     protected static void callObservers() {
          // TODO Add support for mutation observers
     }

     void addSong(long id) {
          // TODO Add handler for duplicate detection
          songIds.add(id);
          save(null);
          callObservers();
     }

     public void addSongs(@NonNull List<Song> songs) {
          // TODO Add handler for duplicate detection
          for (Song song : songs) {
               songIds.add(song.id);
          }
          save(null);
          callObservers();
     }

     public void removeSong(long id) {
          songIds.remove(id);
          save(null);
          callObservers();
     }

     public boolean moveSong(int fromPosition, int toPosition) {
          if (fromPosition == toPosition) {return true;}

          final int size = songIds.size();
          if (fromPosition >= size) {return false;}
          if (toPosition >= size) {return false;}

          final long movedSongId = songIds.get(fromPosition);
          songIds.remove(fromPosition);

          final int toPositionShift = fromPosition < toPosition ? -1 : 0;
          songIds.add(toPosition + toPositionShift, movedSongId);

          save(null);
          callObservers();

          return true;
     }

     void clear() {
          songIds.clear();
          save(null);
          callObservers();
     }

     public void rename(@NonNull final String newName) {
          save(newName);
          callObservers();
     }
}

class PreferencesBackedSongList extends MutableSongList {
     private final static String SEPARATOR = ",";
     private final static String PREF_NAME_PREFIX = "SONG_IDS_";

     private static SharedPreferences preferences = null;
     protected static SharedPreferences getPreferences() {
          if (preferences == null) {
               preferences = PreferenceManager.getDefaultSharedPreferences(App.getStaticContext());
          }
          return preferences;
     }

     static List<PreferencesBackedSongList> loadAll() {
          ArrayList<PreferencesBackedSongList> result = new ArrayList<>();

          final SharedPreferences preferences = getPreferences();
          for (String prefName : preferences.getAll().keySet()) {
               if (prefName.startsWith(PREF_NAME_PREFIX)) {
                    final String name = prefName.substring(PREF_NAME_PREFIX.length());
                    result.add(new PreferencesBackedSongList(name));
               }
          }

          // TODO Sort the result
          // TODO Cache the result
          return result;
     }

     static void remove(@NonNull String name) {
          final SharedPreferences preferences = getPreferences();
          preferences.edit().remove(PREF_NAME_PREFIX + name).apply();

          callObservers();
     }

     PreferencesBackedSongList(@NonNull String name) {
          super(name);
     }

     @Override
     boolean load() {
          final SharedPreferences preferences = getPreferences();
          String values = preferences.getString(PREF_NAME_PREFIX + name, "");

          songIds.clear();
          try {
               for (String id : values.split(SEPARATOR)) {songIds.add(Long.valueOf(id));}
               return true;
          } catch (NumberFormatException ignored) {
               return false;
          }
     }

     @Override
     void save(@Nullable String newName) {
          StringBuilder values = new StringBuilder();
          for (Long id : songIds) {
               if (values.length() > 0) {values.append(SEPARATOR);}
               values.append(id);
          }

          final SharedPreferences preferences = getPreferences();
          if (newName != null) {
               preferences.edit().remove(PREF_NAME_PREFIX + name).apply();
               name = newName;
          }
          preferences.edit().putString(PREF_NAME_PREFIX + name, values.toString()).apply();
     }
}

// TODO SongListFromPlayHistory
