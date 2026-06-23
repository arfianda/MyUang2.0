package com.myuang.app;

import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class TipsActivity extends BaseActivity {
    private MyUangRepository repository;
    private ListenerRegistration userListener;
    private ListenerRegistration transactionListener;
    private TextView summaryText;
    private TextView savingTargetText;
    private TextView savingAchievedText;
    private ProgressBar savingProgress;
    private TextView[] tipTitles;
    private TextView[] tipBodies;
    private double currentSaldo;
    private List<FinanceTransaction> currentTransactions = new ArrayList<>();
    private String lastSavedTipsKey = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tips);
        if (!requireAuthenticated()) {
            return;
        }
        bindBottomNav("tips");

        repository = MyUangRepository.get(this);
        summaryText = findViewById(R.id.textTipsSummary);
        savingTargetText = findViewById(R.id.textSavingTarget);
        savingAchievedText = findViewById(R.id.textSavingAchieved);
        savingProgress = findViewById(R.id.progressSaving);
        tipTitles = new TextView[]{
                findViewById(R.id.textTipTitleOne),
                findViewById(R.id.textTipTitleTwo),
                findViewById(R.id.textTipTitleThree)
        };
        tipBodies = new TextView[]{
                findViewById(R.id.textTipBodyOne),
                findViewById(R.id.textTipBodyTwo),
                findViewById(R.id.textTipBodyThree)
        };

        findViewById(R.id.btnTipsNotification).setOnClickListener(v -> toast(getString(R.string.toast_tips_sync)));
        findViewById(R.id.btnSetCoffeeLimit).setOnClickListener(v -> openScreen(AddTransactionActivity.class));
        findViewById(R.id.btnViewTransport).setOnClickListener(v -> openScreen(AnalyticsActivity.class));
        findViewById(R.id.btnReviewBudget).setOnClickListener(v -> openScreen(AnalyticsActivity.class));

        listenForData();
    }

    @Override
    protected void onDestroy() {
        if (userListener != null) {
            userListener.remove();
        }
        if (transactionListener != null) {
            transactionListener.remove();
        }
        super.onDestroy();
    }

    private void listenForData() {
        userListener = repository.listenUser(new MyUangRepository.UserCallback() {
            @Override
            public void onUser(MyUangRepository.UserProfile user) {
                currentSaldo = user.saldo;
                renderTips();
            }

            @Override
            public void onError(String message) {
                toast(message);
            }
        });

        transactionListener = repository.listenTransactions(new MyUangRepository.TransactionsCallback() {
            @Override
            public void onTransactions(List<FinanceTransaction> transactions) {
                currentTransactions = transactions;
                renderTips();
            }

            @Override
            public void onError(String message) {
                toast(message);
            }
        });
    }

    private void renderTips() {
        AiRecommendationEngine.Result result = AiRecommendationEngine.analyze(currentTransactions, currentSaldo);
        summaryText.setText(result.summary);
        savingTargetText.setText(result.savingTarget);
        savingAchievedText.setText(result.achievedText);
        savingProgress.setProgress(result.savingProgress);

        for (int i = 0; i < tipTitles.length; i++) {
            AiRecommendationEngine.Tip tip = result.tips.get(i);
            tipTitles[i].setText(tip.title);
            tipBodies[i].setText(tip.body);
        }

        String tipsKey = buildTipsKey(result.tips);
        if (!tipsKey.equals(lastSavedTipsKey)) {
            lastSavedTipsKey = tipsKey;
            repository.replaceAiTips(result.tips, (success, message) -> {
                if (!success) {
                    toast(message);
                }
            });
        }
    }

    private String buildTipsKey(List<AiRecommendationEngine.Tip> tips) {
        StringBuilder builder = new StringBuilder();
        for (AiRecommendationEngine.Tip tip : tips) {
            builder.append(tip.title).append('|').append(tip.body).append('\n');
        }
        return builder.toString();
    }
}
