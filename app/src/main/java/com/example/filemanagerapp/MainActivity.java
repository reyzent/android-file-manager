package com.example.filemanagerapp;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSION_WRITE = 1001;
    private boolean permissionGranted;

    private MediaPlayer mPlayer;
    private VideoView videoView;
    private String setType;
    private ImageView imageView;

    private SeekBar audioSeekBar, videoSeekBar;
    private TextView audioCurrentTime, audioTotalTime, videoCurrentTime, videoTotalTime;
    private TextView audioInfo;
    private LinearLayout audioControls, videoControls;
    private Button audioPlay, audioPause, audioStop, videoPlay, videoPause, videoStop, buttonHistory, audioRewindBack, audioRewindForward, videoRewindBack, videoRewindForward;

    private Handler handler = new Handler();
    private boolean isAudioPlaying = false;
    private boolean isVideoPlaying = false;

    private static final int PICKFILE_RESULT_CODE = 1;
    private List<Uri> recentFiles = new ArrayList<>();
    private static final int MAX_HISTORY = 5;
    private static final String HISTORY_PREF = "recent_files";
    private static final int REWIND_TIME = 10000; // 10 секунд

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!permissionGranted) {
            checkPermissions();
        }

        initViews();
        setClickListeners();
    }

    private void initViews() {
        videoView = findViewById(R.id.videoView);

        audioControls = findViewById(R.id.audioControls);
        audioSeekBar = findViewById(R.id.audioSeekBar);
        audioCurrentTime = findViewById(R.id.audioCurrentTime);
        audioTotalTime = findViewById(R.id.audioTotalTime);
        audioInfo = findViewById(R.id.audioInfo);
        audioPlay = findViewById(R.id.audioPlay);
        audioPause = findViewById(R.id.audioPause);
        audioStop = findViewById(R.id.audioStop);

        videoControls = findViewById(R.id.videoControls);
        videoSeekBar = findViewById(R.id.videoSeekBar);
        videoCurrentTime = findViewById(R.id.videoCurrentTime);
        videoTotalTime = findViewById(R.id.videoTotalTime);
        videoPlay = findViewById(R.id.videoPlay);
        videoPause = findViewById(R.id.videoPause);
        videoStop = findViewById(R.id.videoStop);

        audioPause.setEnabled(false);
        audioStop.setEnabled(false);
        videoPause.setEnabled(false);
        videoStop.setEnabled(false);

        // История
        buttonHistory = findViewById(R.id.buttonHistory);

        // Кнопки перемотки для аудио
        audioRewindBack = findViewById(R.id.audioRewindBack);
        audioRewindForward = findViewById(R.id.audioRewindForward);

        // Кнопки перемотки для видео
        videoRewindBack = findViewById(R.id.videoRewindBack);
        videoRewindForward = findViewById(R.id.videoRewindForward);

        // Загружаем историю
        loadHistory();
    }

    private void setClickListeners() {
        Button buttonImage = findViewById(R.id.buttonImage);
        Button buttonAudio = findViewById(R.id.buttonAudio);
        Button buttonVideo = findViewById(R.id.buttonVideo);

        buttonImage.setOnClickListener(this::onClick);
        buttonAudio.setOnClickListener(this::onClick);
        buttonVideo.setOnClickListener(this::onClick);

        // История и перемотка
        buttonHistory.setOnClickListener(v -> showHistoryDialog());
        audioRewindBack.setOnClickListener(v -> rewindAudio(-REWIND_TIME));
        audioRewindForward.setOnClickListener(v -> rewindAudio(REWIND_TIME));
        videoRewindBack.setOnClickListener(v -> rewindVideo(-REWIND_TIME));
        videoRewindForward.setOnClickListener(v -> rewindVideo(REWIND_TIME));

        audioPlay.setOnClickListener(this::audioPlay);
        audioPause.setOnClickListener(this::audioPause);
        audioStop.setOnClickListener(this::audioStop);

        videoPlay.setOnClickListener(this::videoPlay);
        videoPause.setOnClickListener(this::videoPause);
        videoStop.setOnClickListener(this::videoStop);

        setupSeekBars();
    }

    private void setupSeekBars() {
        audioSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mPlayer != null) {
                    mPlayer.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        videoSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && videoView.isPlaying()) {
                    videoView.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    public boolean isExternalStorageWriteable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return (Environment.MEDIA_MOUNTED.equals(state) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state));
    }

    private boolean checkPermissions() {
        if (!isExternalStorageReadable() || !isExternalStorageWriteable()) {
            Toast.makeText(this, "Внешнее хранилище не доступно", Toast.LENGTH_LONG).show();
            return false;
        }
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION_WRITE);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_PERMISSION_WRITE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    permissionGranted = true;
                    Toast.makeText(this, "Разрешения получены", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Необходимо дать разрешения", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    public void onClick(View viewButton) {
        if (viewButton.getId() == R.id.buttonAudio) {
            setType = "audio/*";
        }
        if (viewButton.getId() == R.id.buttonVideo) {
            setType = "video/*";
        }
        if (viewButton.getId() == R.id.buttonImage) {
            setType = "image/*";
        }
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType(setType);
        startActivityForResult(intent, PICKFILE_RESULT_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICKFILE_RESULT_CODE && resultCode == RESULT_OK) {
            if (setType.equals("audio/*")) {
                setupAudioPlayer(data.getData());
            }
            if (setType.equals("video/*")) {
                setupVideoPlayer(data.getData());
            }
            if (setType.equals("image/*")) {
                setContentView(R.layout.imageview);
                imageView = findViewById(R.id.imageView);
                imageView.setImageURI(data.getData());
            }
        }
    }

    private void setupAudioPlayer(Uri audioUri) {
        try {
            if (mPlayer != null) {
                mPlayer.release();
            }

            mPlayer = MediaPlayer.create(this, audioUri);
            mPlayer.setOnCompletionListener(mp -> stopAudioPlay());

            audioSeekBar.setMax(mPlayer.getDuration());
            audioTotalTime.setText(formatTime(mPlayer.getDuration()));
            audioInfo.setText("Аудиофайл загружен");

            audioControls.setVisibility(View.VISIBLE);
            videoControls.setVisibility(View.GONE);
            videoView.setVisibility(View.GONE);

            startAudioProgressUpdate();

            audioPlay.setEnabled(true);
            audioPause.setEnabled(false);
            audioStop.setEnabled(true);

            // Добавляем в историю
            addToHistory(audioUri);

        } catch (Exception e) {
            Toast.makeText(this, "Ошибка загрузки аудио: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void setupVideoPlayer(Uri videoUri) {
        try {
            videoView.setVideoURI(videoUri);

            videoControls.setVisibility(View.VISIBLE);
            audioControls.setVisibility(View.GONE);
            videoView.setVisibility(View.VISIBLE);

            videoPlay.setEnabled(true);
            videoPause.setEnabled(false);
            videoStop.setEnabled(true);

            // Добавляем в историю
            addToHistory(videoUri);

            videoView.setOnPreparedListener(mp -> {
                videoSeekBar.setMax(videoView.getDuration());
                videoTotalTime.setText(formatTime(videoView.getDuration()));
                startVideoProgressUpdate();
            });

        } catch (Exception e) {
            Toast.makeText(this, "Ошибка загрузки видео: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public void audioPlay(View view) {
        if (mPlayer != null && !mPlayer.isPlaying()) {
            mPlayer.start();
            audioPlay.setEnabled(false);
            audioPause.setEnabled(true);
            isAudioPlaying = true;
            startAudioProgressUpdate();
        }
    }

    public void audioPause(View view) {
        if (mPlayer != null && mPlayer.isPlaying()) {
            mPlayer.pause();
            audioPlay.setEnabled(true);
            audioPause.setEnabled(false);
            isAudioPlaying = false;
        }
    }

    public void audioStop(View view) {
        stopAudioPlay();
    }

    private void stopAudioPlay() {
        if (mPlayer != null) {
            mPlayer.stop();
            try {
                mPlayer.prepare();
                mPlayer.seekTo(0);
                audioSeekBar.setProgress(0);
                audioCurrentTime.setText(formatTime(0));

                audioPlay.setEnabled(true);
                audioPause.setEnabled(false);
                audioStop.setEnabled(false);
                isAudioPlaying = false;

            } catch (Exception e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void videoPlay(View view) {
        if (!videoView.isPlaying()) {
            // Если видео почти закончилось, сбрасываем на начало
            if (videoView.getCurrentPosition() >= videoView.getDuration() - 100) {
                videoView.seekTo(0);
            }
            videoView.start();
            videoPlay.setEnabled(false);
            videoPause.setEnabled(true);
            videoStop.setEnabled(true);
            isVideoPlaying = true;
            startVideoProgressUpdate();
        }
    }

    public void videoPause(View view) {
        if (videoView.isPlaying()) {
            videoView.pause();
            videoPlay.setEnabled(true);
            videoPause.setEnabled(false);
            isVideoPlaying = false;
        }
    }

    public void videoStop(View view) {
        if (videoView.isPlaying() || videoView.getCurrentPosition() > 0) {
            videoView.pause();
            videoView.seekTo(0); // Сбрасываем на начало

            videoSeekBar.setProgress(0);
            videoCurrentTime.setText(formatTime(0));

            videoPlay.setEnabled(true);
            videoPause.setEnabled(false);
            videoStop.setEnabled(false);
            isVideoPlaying = false;

            // Останавливаем обновление прогресса
            handler.removeCallbacks(videoProgressUpdater);
        }
    }

    private void startAudioProgressUpdate() {
        handler.postDelayed(audioProgressUpdater, 1000);
    }

    private void startVideoProgressUpdate() {
        handler.postDelayed(videoProgressUpdater, 1000);
    }

    private Runnable audioProgressUpdater = new Runnable() {
        public void run() {
            if (mPlayer != null && isAudioPlaying) {
                int currentPosition = mPlayer.getCurrentPosition();
                audioSeekBar.setProgress(currentPosition);
                audioCurrentTime.setText(formatTime(currentPosition));
                handler.postDelayed(this, 1000);
            }
        }
    };

    private Runnable videoProgressUpdater = new Runnable() {
        public void run() {
            if (videoView.isPlaying()) {
                int currentPosition = videoView.getCurrentPosition();
                int duration = videoView.getDuration();

                videoSeekBar.setProgress(currentPosition);
                videoCurrentTime.setText(formatTime(currentPosition));

                // Если видео закончилось, останавливаем
                if (currentPosition >= duration - 100) {
                    videoStop(null);
                } else {
                    handler.postDelayed(this, 1000);
                }
            }
        }
    };

    private String formatTime(int milliseconds) {
        int seconds = (milliseconds / 1000) % 60;
        int minutes = (milliseconds / (1000 * 60)) % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    // ==================== ИСТОРИЯ ФАЙЛОВ ====================

    private void loadHistory() {
        // Пока просто инициализируем пустой список
        // Позже можно добавить сохранение в SharedPreferences
        recentFiles = new ArrayList<>();
    }

    private void addToHistory(Uri fileUri) {
        // Удаляем если уже есть в истории (сравниваем по строке)
        for (int i = 0; i < recentFiles.size(); i++) {
            if (recentFiles.get(i).toString().equals(fileUri.toString())) {
                recentFiles.remove(i);
                break;
            }
        }

        // Добавляем в начало
        recentFiles.add(0, fileUri);

        // Ограничиваем размер истории
        if (recentFiles.size() > MAX_HISTORY) {
            recentFiles = recentFiles.subList(0, MAX_HISTORY);
        }
    }

    private void showHistoryDialog() {
        if (recentFiles.isEmpty()) {
            Toast.makeText(this, "Нет недавних файлов", Toast.LENGTH_SHORT).show();
            return;
        }

        // Создаем диалог
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Недавние файлы");

        // Создаем массив названий файлов
        String[] fileNames = new String[recentFiles.size()];
        for (int i = 0; i < recentFiles.size(); i++) {
            Uri fileUri = recentFiles.get(i);
            String fileName = getFileNameFromUri(fileUri);
            fileNames[i] = fileName != null ? fileName : "Файл " + (i + 1);
        }

        builder.setItems(fileNames, (dialog, which) -> {
            Uri selectedUri = recentFiles.get(which);
            openFileFromHistory(selectedUri);
        });

        builder.setNegativeButton("Закрыть", null);
        builder.setPositiveButton("Очистить историю", (dialog, which) -> clearHistory());

        builder.show();
    }

    // Получение имени файла из Uri
    private String getFileNameFromUri(Uri uri) {
        String fileName = null;
        String scheme = uri.getScheme();

        if (scheme != null && scheme.equals("content")) {
            // Для content URI используем ContentResolver
            try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (scheme != null && scheme.equals("file")) {
            // Для file URI берем последнюю часть пути
            fileName = uri.getLastPathSegment();
        }

        return fileName;
    }

    private void openFileFromHistory(Uri fileUri) {
        String uriString = fileUri.toString().toLowerCase();

        if (uriString.endsWith(".mp3") || uriString.endsWith(".wav") || uriString.endsWith(".aac")) {
            setType = "audio/*";
            setupAudioPlayer(fileUri);
        } else if (uriString.endsWith(".mp4") || uriString.endsWith(".avi") || uriString.endsWith(".mkv")) {
            setType = "video/*";
            setupVideoPlayer(fileUri);
        } else if (uriString.endsWith(".jpg") || uriString.endsWith(".png") || uriString.endsWith(".jpeg")) {
            setType = "image/*";
            setContentView(R.layout.imageview);
            imageView = findViewById(R.id.imageView);
            imageView.setImageURI(fileUri);
        } else {
            Toast.makeText(this, "Неизвестный тип файла", Toast.LENGTH_SHORT).show();
        }
    }

    private void clearHistory() {
        recentFiles.clear();
        Toast.makeText(this, "История очищена", Toast.LENGTH_SHORT).show();
    }

    // ==================== БЫСТРАЯ ПЕРЕМОТКА ====================

    private void rewindAudio(int timeMs) {
        if (mPlayer != null && mPlayer.isPlaying()) {
            int currentPosition = mPlayer.getCurrentPosition();
            int newPosition = currentPosition + timeMs;

            // Проверяем границы
            if (newPosition < 0) newPosition = 0;
            if (newPosition > mPlayer.getDuration()) newPosition = mPlayer.getDuration();

            mPlayer.seekTo(newPosition);
            audioSeekBar.setProgress(newPosition);
            audioCurrentTime.setText(formatTime(newPosition));

            Toast.makeText(this, timeMs > 0 ? "+10 сек" : "-10 сек", Toast.LENGTH_SHORT).show();
        }
    }

    private void rewindVideo(int timeMs) {
        if (videoView.isPlaying()) {
            int currentPosition = videoView.getCurrentPosition();
            int newPosition = currentPosition + timeMs;

            // Проверяем границы
            if (newPosition < 0) newPosition = 0;
            if (newPosition > videoView.getDuration()) newPosition = videoView.getDuration();

            videoView.seekTo(newPosition);
            videoSeekBar.setProgress(newPosition);
            videoCurrentTime.setText(formatTime(newPosition));

            Toast.makeText(this, timeMs > 0 ? "+10 сек" : "-10 сек", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPlayer != null) {
            mPlayer.release();
        }
        handler.removeCallbacks(audioProgressUpdater);
        handler.removeCallbacks(videoProgressUpdater);
    }
}