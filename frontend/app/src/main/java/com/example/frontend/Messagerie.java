package com.example.frontend;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.Manifest;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.frontend.adapter.MessageAdapter;
import com.example.frontend.component.Participants;
import com.example.frontend.model.Message;
import com.example.frontend.model.MessagePJ;
import com.example.frontend.model.User;
import com.example.frontend.utils.Constants;
import com.example.frontend.utils.TokenManager;
import com.example.frontend.utils.UIUtils;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import android.os.Handler;


public class Messagerie extends AppCompatActivity implements MessageAdapter.OnAttachmentDownloadListener {

    private TokenManager tokenManager;
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    private ProgressDialog progressDialog;
    private List<User> users = new ArrayList<>();
    private List<Message> messages = new ArrayList<>();
    private MessageAdapter adapter;

    private Socket socket;

    private long groupId;
    private long userId;

    ExecutorService executor;

    RecyclerView messageRecycler;
    private ActivityResultLauncher<Intent> filePickerLauncher;
    private List<MessagePJ> attachments = new ArrayList<>();
    private LinearLayout pjContainer;

    private MessagePJ pendingPJToDownload;

    private static final int REQUEST_CODE_WRITE_STORAGE = 100;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_messagerie);
        UIUtils.hideSystemUI(this);
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            try {
                                InputStream inputStream = getContentResolver().openInputStream(uri);
                                byte[] bytes = new byte[inputStream.available()];
                                inputStream.read(bytes);
                                inputStream.close();

                                String base64 = Base64.encodeToString(bytes, Base64.DEFAULT);
                                String fileName = getFileNameFromUri(uri);

                                // Créer l'objet MessagePJ
                                MessagePJ pj = new MessagePJ();
                                pj.setByte64(base64);
                                pj.setDate(new Date());
                                pj.setName(fileName);
                                attachments.add(pj);

                                addAttachmentToLayout(fileName);

                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
        );
        tokenManager = new TokenManager(this);
        tokenManager.CheckConnexion();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // <== fait scroller tout en bas automatiquement
        pjContainer = findViewById(R.id.pj_container);
        groupId = getIntent().getLongExtra("groupId", 0);
        userId = tokenManager.getUserId();

        Participants participantsView = findViewById(R.id.participants);
        messageRecycler = findViewById(R.id.message_list_recycler);
        messageRecycler.setLayoutManager(layoutManager);

        adapter = new MessageAdapter(userId);
        adapter.setOnAttachmentDownloadListener(this);
        messageRecycler.setAdapter(adapter);

        executor = Executors.newSingleThreadExecutor();

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Chargement...");
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminate(true);
        progressDialog.show();
        ImageButton send_button = findViewById(R.id.send_button);
        EditText send_content = findViewById(R.id.send_input);
        send_button.setOnClickListener(v -> {
            String content = send_content.getText().toString();
            if(content.isEmpty()){
                Toast.makeText(this, "Impossible d'envoyer un message vide", Toast.LENGTH_LONG).show();
                return;
            }
            sendMessage(content, send_content);
        });

        fetchGroup(participantsView, groupId);
        ImageButton send_addPj = findViewById(R.id.send_add_pj);
        send_addPj.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            filePickerLauncher.launch(intent);
        });
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_CODE_WRITE_STORAGE);
            }
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialisation Socket.IO
        try {
            IO.Options opts = new IO.Options();
            opts.reconnection = true;
            opts.timeout = 10000;
            opts.query = "token=" + tokenManager.getAccessToken();

            socket = IO.socket(Constants.WS_BASE_URL, opts);

            socket.on(Socket.EVENT_CONNECT, args -> runOnUiThread(() -> {
                Log.d("SOCKET.IO", "Connected to server");
                // Join room / groupe
                JSONObject joinPayload = new JSONObject();
                try {
                    joinPayload.put("groupId", groupId);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                socket.emit("join_group", joinPayload);
            }));

            socket.on(Socket.EVENT_DISCONNECT, args -> runOnUiThread(() ->
                    Log.d("SOCKET.IO", "Disconnected from server")));

            socket.on("new_message", args -> runOnUiThread(() -> {
                Log.d("SOCKET.IO", "Message reçu ");
                JSONObject responseJson = (JSONObject) args[0];
                Log.d("SOCKET.IO", responseJson.toString());
                try {
                    JSONObject messageJson = responseJson.getJSONObject("message");
                    long incomingGroupId = messageJson.getLong("groupId");
                    if (incomingGroupId == groupId) {
                        Message message = new Message();
                        message.JsonToMessage(messageJson);
                        executor.execute(() -> {
                            User user = fetchUserByIdSync(message.getUserId());
                            message.setUser(user);
                            new Handler(Looper.getMainLooper()).post(() -> {
                                adapter.addMessage(message);
                                messageRecycler.scrollToPosition(adapter.getItemCount() - 1);
                            });
                        });
                        messageRecycler.scrollToPosition(adapter.getItemCount() - 1);
                        Log.d("SOCKET.IO", "Message dans le recyclerView");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.d("SOCKET.IO", e.toString());
                }
            }));

            socket.connect();

        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private void fetchGroup(Participants participantsView, long groupId) {
        Log.d("API CALL", "Trying to fetch group n°" + groupId);
        String url = Constants.API_BASE_URL + "/group/" + groupId;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "Bearer " + tokenManager.getAccessToken())
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(Messagerie.this, "Erreur de connexion", Toast.LENGTH_SHORT).show();
                    Log.d("API CALL", e.toString());
                });
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        JSONObject group = (new JSONObject(response.body().string())).getJSONObject("group");
                        response.body().close();
                        JSONArray usersArray = group.getJSONArray("users");
                        Log.d("API CALL", usersArray.toString());
                        users.clear();
                        for (int i = 0; i < usersArray.length(); i++) {
                            JSONObject userJson = usersArray.getJSONObject(i);
                            User user = new User();
                            user.JsonToUser(userJson);
                            users.add(user);
                        }
                        JSONArray messagesArray = group.getJSONArray("messages");
                        Log.d("API CALL", messagesArray.toString());
                        messages.clear();
                        executor.execute(() -> {
                            try{
                                List<Message> loadedMessages = new ArrayList<>();
                                for (int i = messagesArray.length() - 1; i >= 0; i--) {
                                    JSONObject messageJson = messagesArray.getJSONObject(i);
                                    Message message = new Message();
                                    message.JsonToMessage(messageJson);
                                    User user = fetchUserByIdSync(message.getUserId());
                                    message.setUser(user);
                                    loadedMessages.add(message);
                                }
                                runOnUiThread(() -> {
                                    adapter.setMessages(loadedMessages);
                                    messageRecycler.scrollToPosition(adapter.getItemCount() - 1);
                                });
                            } catch (JSONException e)
                            {
                                e.printStackTrace();
                                Log.d("Messagerie", e.toString());
                            }

                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.d("API CALL", e.toString());
                        runOnUiThread(() -> {
                            Toast.makeText(Messagerie.this, "Erreur serveur : " + response.code(), Toast.LENGTH_SHORT).show();
                            progressDialog.dismiss();
                        });
                    } finally {
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            participantsView.setParticipants(users);
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        Log.d("API CALL", response.message());
                        Toast.makeText(Messagerie.this, "Erreur serveur : " + response.code(), Toast.LENGTH_SHORT).show();
                        progressDialog.dismiss();
                    });
                }
            }
        });
    }
    private void sendMessage(String content, EditText send_input) {
        if (socket != null && socket.connected()) {
            JSONObject msg = new JSONObject();
            try {
                msg.put("groupId", groupId);
                msg.put("userId", userId);
                msg.put("content", content);
                JSONArray attachmentsArray = new JSONArray();
                for (MessagePJ pj : attachments) {
                    JSONObject pjJson = new JSONObject();
                    pjJson.put("name", pj.getName());
                    pjJson.put("byte64", pj.getByte64());
                    pjJson.put("date", pj.getDate().getTime());
                    attachmentsArray.put(pjJson);
                }
                msg.put("pieces_jointe", attachmentsArray);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            socket.emit("send_message", msg);
            send_input.setText("");
            attachments.clear();
            pjContainer.removeAllViews();
        } else {
            Toast.makeText(this, "Connexion au serveur perdue", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (socket != null) {
            // Quitter la room avant de fermer
            JSONObject leavePayload = new JSONObject();
            try {
                leavePayload.put("groupId", groupId);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            socket.emit("leave_group", leavePayload);

            socket.disconnect();
            socket.off();
        }
    }

    public User fetchUserByIdSync(long userId) {
        String url = Constants.API_BASE_URL + "/user/" + userId;
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + tokenManager.getAccessToken())
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String body = response.body().string();
                JSONObject userJson = new JSONObject(body);
                User user = new User();
                user.JsonToUser(userJson);
                return user;
            }
        } catch (Exception e) {
            Log.e("SYNC_FETCH", e.toString());
        }

        return null;
    }
    private String getFileNameFromUri(Uri uri) {
        String result = null;

        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        if (result == null && uri.getLastPathSegment() != null) {
            result = uri.getLastPathSegment();
        }

        return result;
    }

    private void addAttachmentToLayout(String fileName) {
        View attachmentView = LayoutInflater.from(this).inflate(R.layout.item_attachment_messagerie, pjContainer, false);

        TextView fileNameTextView = attachmentView.findViewById(R.id.attachment_filename);
        ImageView deleteButton = attachmentView.findViewById(R.id.attachment_remove);

        fileNameTextView.setText(fileName);

        // Stocke l’index actuel dans une variable finale pour la suppression
        int index = attachments.size() - 1;

        deleteButton.setOnClickListener(v -> {
            // Supprime de la liste des pièces jointes
            if (index >= 0 && index < attachments.size()) {
                attachments.remove(index);
            }
            // Supprime la vue de l’interface
            pjContainer.removeView(attachmentView);
        });

        pjContainer.addView(attachmentView);
    }

    @Override
    public void onDownloadRequested(MessagePJ pj) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) { // Android 10 = API 29
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                pendingPJToDownload = pj;
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_CODE_WRITE_STORAGE);
                return;
            }
        }
        saveFile(pj);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_WRITE_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (pendingPJToDownload != null) {
                    saveFile(pendingPJToDownload);
                    pendingPJToDownload = null;
                }
            } else {
                Toast.makeText(this, "Permission refusée, autorisé l'application à lire et écrire sur votre téléphone", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveFile(MessagePJ pj) {
        try {
            byte[] fileBytes = Base64.decode(pj.getByte64(), Base64.DEFAULT);
            String fileName = pj.getName();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, UIUtils.getMimeType(fileName));
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                ContentResolver resolver = getContentResolver();
                Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri != null) {
                    try (OutputStream out = resolver.openOutputStream(uri)) {
                        out.write(fileBytes);
                    }
                }
            } else {
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File file = new File(downloadsDir, fileName);
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(fileBytes);
                }
                // Notifier le media scanner pour le rendre visible
                Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                intent.setData(Uri.fromFile(file));
                sendBroadcast(intent);
            }

            Toast.makeText(this, "Fichier téléchargé : " + fileName, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Erreur lors du téléchargement", Toast.LENGTH_SHORT).show();
        }
    }
}
