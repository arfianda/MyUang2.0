package com.myuang.app;

import android.content.Context;

import com.google.firebase.FirebaseApp;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyUangRepository {
    private static MyUangRepository instance;

    private final Context appContext;
    private final FirebaseAuth auth;
    private final FirebaseFirestore db;
    private final boolean configured;
    private final String configurationMessage;

    public interface CompletionCallback {
        void onComplete(boolean success, String message);
    }

    public interface UserCallback {
        void onUser(UserProfile user);

        void onError(String message);
    }

    public interface TransactionsCallback {
        void onTransactions(List<FinanceTransaction> transactions);

        void onError(String message);
    }

    public static class UserProfile {
        public String uid;
        public String name;
        public String email;
        public double saldo;
    }

    private MyUangRepository(Context context) {
        appContext = context.getApplicationContext();
        boolean isConfigured = false;
        FirebaseAuth firebaseAuth = null;
        FirebaseFirestore firestore = null;
        String message = "";

        try {
            if (FirebaseApp.getApps(appContext).isEmpty()) {
                FirebaseApp.initializeApp(appContext);
            }
            isConfigured = !FirebaseApp.getApps(appContext).isEmpty();
            if (isConfigured) {
                firebaseAuth = FirebaseAuth.getInstance();
                firestore = FirebaseFirestore.getInstance();
            } else {
                message = "Firebase belum dikonfigurasi. Tambahkan app/google-services.json dari Firebase Console.";
            }
        } catch (IllegalStateException exception) {
            message = "Firebase belum dikonfigurasi. Tambahkan app/google-services.json dari Firebase Console.";
        }

        configured = isConfigured;
        auth = firebaseAuth;
        db = firestore;
        configurationMessage = message;
    }

    public static synchronized MyUangRepository get(Context context) {
        if (instance == null) {
            instance = new MyUangRepository(context);
        }
        return instance;
    }

    public boolean isConfigured() {
        return configured;
    }

    public String getConfigurationMessage() {
        return configurationMessage;
    }

    public boolean isLoggedIn() {
        return configured && auth.getCurrentUser() != null;
    }

    public String currentUserId() {
        FirebaseUser user = configured ? auth.getCurrentUser() : null;
        return user == null ? null : user.getUid();
    }

    public void signIn(String email, String password, CompletionCallback callback) {
        if (!ensureConfigured(callback)) {
            return;
        }
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> ensureUserDocument(result, callback))
                .addOnFailureListener(error -> callback.onComplete(false, cleanError(error)));
    }

    public void register(String name, String email, String password, double saldo, CompletionCallback callback) {
        if (!ensureConfigured(callback)) {
            return;
        }
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser firebaseUser = result.getUser();
                    if (firebaseUser == null) {
                        callback.onComplete(false, "Akun gagal dibuat. Coba lagi.");
                        return;
                    }

                    Map<String, Object> userData = new HashMap<>();
                    userData.put("uid", firebaseUser.getUid());
                    userData.put("name", name);
                    userData.put("email", email);
                    userData.put("saldo", saldo);
                    userData.put("createdAt", FieldValue.serverTimestamp());
                    userData.put("updatedAt", FieldValue.serverTimestamp());

                    db.collection("users").document(firebaseUser.getUid())
                            .set(userData)
                            .addOnSuccessListener(unused -> callback.onComplete(true, "Akun berhasil dibuat."))
                            .addOnFailureListener(error -> callback.onComplete(false, cleanError(error)));
                })
                .addOnFailureListener(error -> callback.onComplete(false, cleanError(error)));
    }

    public void sendPasswordReset(String email, CompletionCallback callback) {
        if (!ensureConfigured(callback)) {
            return;
        }
        auth.sendPasswordResetEmail(email)
                .addOnSuccessListener(unused -> callback.onComplete(true, "Email reset kata sandi sudah dikirim."))
                .addOnFailureListener(error -> callback.onComplete(false, cleanError(error)));
    }

    public void logout() {
        if (configured) {
            auth.signOut();
        }
    }

    public void updateSaldo(double saldo, CompletionCallback callback) {
        if (!ensureConfigured(callback)) {
            return;
        }
        String uid = currentUserId();
        if (uid == null) {
            callback.onComplete(false, "Sesi login tidak ditemukan.");
            return;
        }
        Map<String, Object> fields = new HashMap<>();
        fields.put("saldo", saldo);
        fields.put("updatedAt", FieldValue.serverTimestamp());
        db.collection("users").document(uid)
                .set(fields, SetOptions.merge())
                .addOnSuccessListener(unused -> callback.onComplete(true, "Saldo berhasil diperbarui."))
                .addOnFailureListener(error -> callback.onComplete(false, cleanError(error)));
    }

    public void updateName(String name, CompletionCallback callback) {
        if (!ensureConfigured(callback)) {
            return;
        }
        String uid = currentUserId();
        if (uid == null) {
            callback.onComplete(false, "Sesi login tidak ditemukan.");
            return;
        }
        Map<String, Object> fields = new HashMap<>();
        fields.put("name", name);
        fields.put("updatedAt", FieldValue.serverTimestamp());
        db.collection("users").document(uid)
                .set(fields, SetOptions.merge())
                .addOnSuccessListener(unused -> callback.onComplete(true, "Nama berhasil diperbarui."))
                .addOnFailureListener(error -> callback.onComplete(false, cleanError(error)));
    }

    public void addTransaction(String type, String category, double amount, String note, String label,
                               Date createdAt, CompletionCallback callback) {
        if (!ensureConfigured(callback)) {
            return;
        }
        String uid = currentUserId();
        if (uid == null) {
            callback.onComplete(false, "Sesi login tidak ditemukan.");
            return;
        }

        DocumentReference transactionRef = db.collection("transactions").document();
        Map<String, Object> data = new HashMap<>();
        data.put("transactionId", transactionRef.getId());
        data.put("userId", uid);
        data.put("type", type);
        data.put("category", category);
        data.put("amount", amount);
        data.put("note", note);
        data.put("label", label);
        data.put("createdAt", createdAt == null ? new Date() : createdAt);
        data.put("updatedAt", FieldValue.serverTimestamp());

        double saldoDelta = FinanceTransaction.TYPE_INCOME.equals(type) ? amount : -amount;
        WriteBatch batch = db.batch();
        batch.set(transactionRef, data);
        batch.set(db.collection("users").document(uid), createSaldoUpdate(saldoDelta), SetOptions.merge());
        batch.commit()
                .addOnSuccessListener(unused -> callback.onComplete(true, "Transaksi berhasil disimpan."))
                .addOnFailureListener(error -> callback.onComplete(false, cleanError(error)));
    }

    public ListenerRegistration listenUser(UserCallback callback) {
        if (!configured) {
            callback.onError(configurationMessage);
            return null;
        }
        String uid = currentUserId();
        if (uid == null) {
            callback.onError("Sesi login tidak ditemukan.");
            return null;
        }
        return db.collection("users").document(uid).addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                callback.onError(cleanError(error));
                return;
            }
            if (snapshot == null || !snapshot.exists()) {
                callback.onError("Profil pengguna belum tersedia.");
                return;
            }
            callback.onUser(toUserProfile(snapshot));
        });
    }

    public ListenerRegistration listenTransactions(TransactionsCallback callback) {
        if (!configured) {
            callback.onError(configurationMessage);
            return null;
        }
        String uid = currentUserId();
        if (uid == null) {
            callback.onError("Sesi login tidak ditemukan.");
            return null;
        }
        return db.collection("transactions")
                .whereEqualTo("userId", uid)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        callback.onError(cleanError(error));
                        return;
                    }

                    List<FinanceTransaction> transactions = new ArrayList<>();
                    if (snapshot != null) {
                        for (QueryDocumentSnapshot document : snapshot) {
                            transactions.add(toTransaction(document));
                        }
                    }
                    Collections.sort(transactions, (left, right) -> right.createdAt.compareTo(left.createdAt));
                    callback.onTransactions(transactions);
                });
    }

    public void replaceAiTips(List<AiRecommendationEngine.Tip> tips, CompletionCallback callback) {
        if (!ensureConfigured(callback)) {
            return;
        }
        String uid = currentUserId();
        if (uid == null) {
            callback.onComplete(false, "Sesi login tidak ditemukan.");
            return;
        }

        db.collection("ai_tips")
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        batch.delete(document.getReference());
                    }
                    for (AiRecommendationEngine.Tip tip : tips) {
                        DocumentReference tipRef = db.collection("ai_tips").document();
                        Map<String, Object> data = new HashMap<>();
                        data.put("tipId", tipRef.getId());
                        data.put("userId", uid);
                        data.put("title", tip.title);
                        data.put("content", tip.body);
                        data.put("category", tip.category);
                        data.put("createdAt", FieldValue.serverTimestamp());
                        batch.set(tipRef, data);
                    }
                    batch.commit()
                            .addOnSuccessListener(unused -> callback.onComplete(true, "Tips AI diperbarui."))
                            .addOnFailureListener(error -> callback.onComplete(false, cleanError(error)));
                })
                .addOnFailureListener(error -> callback.onComplete(false, cleanError(error)));
    }

    private void ensureUserDocument(AuthResult result, CompletionCallback callback) {
        FirebaseUser firebaseUser = result.getUser();
        if (firebaseUser == null) {
            callback.onComplete(false, "Login gagal. Coba lagi.");
            return;
        }

        DocumentReference userRef = db.collection("users").document(firebaseUser.getUid());
        userRef.get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        callback.onComplete(true, "Login berhasil.");
                        return;
                    }

                    Map<String, Object> userData = new HashMap<>();
                    userData.put("uid", firebaseUser.getUid());
                    userData.put("name", fallbackName(firebaseUser));
                    userData.put("email", firebaseUser.getEmail());
                    userData.put("saldo", 0);
                    userData.put("createdAt", FieldValue.serverTimestamp());
                    userData.put("updatedAt", FieldValue.serverTimestamp());
                    userRef.set(userData)
                            .addOnSuccessListener(unused -> callback.onComplete(true, "Login berhasil."))
                            .addOnFailureListener(error -> callback.onComplete(false, cleanError(error)));
                })
                .addOnFailureListener(error -> callback.onComplete(false, cleanError(error)));
    }

    private Map<String, Object> createSaldoUpdate(double saldoDelta) {
        Map<String, Object> update = new HashMap<>();
        update.put("saldo", FieldValue.increment(saldoDelta));
        update.put("updatedAt", FieldValue.serverTimestamp());
        return update;
    }

    private UserProfile toUserProfile(DocumentSnapshot snapshot) {
        UserProfile profile = new UserProfile();
        profile.uid = valueOrEmpty(snapshot.getString("uid"));
        profile.name = valueOrEmpty(snapshot.getString("name"));
        profile.email = valueOrEmpty(snapshot.getString("email"));
        profile.saldo = numberValue(snapshot.get("saldo"));
        if (profile.name.isEmpty()) {
            FirebaseUser currentUser = auth.getCurrentUser();
            profile.name = currentUser == null ? "Pengguna MyUang" : fallbackName(currentUser);
        }
        return profile;
    }

    private FinanceTransaction toTransaction(DocumentSnapshot snapshot) {
        FinanceTransaction transaction = new FinanceTransaction();
        transaction.transactionId = valueOrEmpty(snapshot.getString("transactionId"));
        if (transaction.transactionId.isEmpty()) {
            transaction.transactionId = snapshot.getId();
        }
        transaction.userId = valueOrEmpty(snapshot.getString("userId"));
        transaction.type = valueOrEmpty(snapshot.getString("type"));
        transaction.category = valueOrEmpty(snapshot.getString("category"));
        transaction.note = valueOrEmpty(snapshot.getString("note"));
        transaction.label = valueOrEmpty(snapshot.getString("label"));
        transaction.amount = numberValue(snapshot.get("amount"));
        transaction.createdAt = dateValue(snapshot.get("createdAt"));
        return transaction;
    }

    private boolean ensureConfigured(CompletionCallback callback) {
        if (configured) {
            return true;
        }
        callback.onComplete(false, configurationMessage);
        return false;
    }

    private static Date dateValue(Object value) {
        if (value instanceof Timestamp) {
            return ((Timestamp) value).toDate();
        }
        if (value instanceof Date) {
            return (Date) value;
        }
        return new Date(0);
    }

    private static double numberValue(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0;
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String fallbackName(FirebaseUser firebaseUser) {
        String email = firebaseUser.getEmail();
        if (email == null || email.trim().isEmpty()) {
            return "Pengguna MyUang";
        }
        int atIndex = email.indexOf('@');
        return atIndex > 0 ? email.substring(0, atIndex) : email;
    }

    private static String cleanError(Exception error) {
        String message = error.getMessage();
        return message == null || message.trim().isEmpty()
                ? "Operasi gagal. Coba lagi."
                : message;
    }
}
