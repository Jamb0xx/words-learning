package com.example.wordkid;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {
    private static final int SESSION_SIZE = 20;
    private final Random random = new Random();
    private DBHelper db;
    private TextToSpeech tts;
    private boolean isTtsReady = false;
    private SharedPreferences prefs;
    private boolean onHome = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new DBHelper(this);
        prefs = getSharedPreferences("stats", MODE_PRIVATE);
        tts = new TextToSpeech(this, this);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (!onHome) {
                    showHome();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        showHome();
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int res = tts.setLanguage(Locale.US);
            isTtsReady = (res != TextToSpeech.LANG_MISSING_DATA && res != TextToSpeech.LANG_NOT_SUPPORTED);
            tts.setSpeechRate(0.85f);
        } else {
            isTtsReady = false;
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        db.close();
        super.onDestroy();
    }

    private void showHome() {
        onHome = true;
        LinearLayout root = page();
        TextView title = title("WordKid English");
        root.addView(title);
        TextView subtitle = text("Учи слова понемногу каждый день");
        subtitle.setTextSize(17);
        subtitle.setGravity(Gravity.CENTER_HORIZONTAL);
        subtitle.setPadding(0, 0, 0, dp(20));
        root.addView(subtitle);

        root.addView(menuButton("📚  Карточки", v -> startCards()));
        root.addView(menuButton("✍️  Русский → English", v -> startTyping(false)));
        root.addView(menuButton("🔤  English → русский", v -> startChoice()));
        root.addView(menuButton("🔊  На слух → написать", v -> startTyping(true)));
        root.addView(menuButton("➕  Мои слова", v -> showWords()));
        root.addView(menuButton("⭐  Статистика", v -> showStats()));

        TextView count = text("Слов в словаре: " + db.getAllWords().size());
        count.setGravity(Gravity.CENTER);
        count.setPadding(0, dp(18), 0, 0);
        root.addView(count);
        setContentView(wrap(root));
    }

    private void startCards() {
        List<Word> session = sessionWords();
        if (session.isEmpty()) {
            Toast.makeText(this, "Словарь пуст! Добавьте слова в Мои слова.", Toast.LENGTH_SHORT).show();
            return;
        }
        onHome = false;
        showCard(session, 0, false);
    }

    private void showCard(List<Word> session, int index, boolean revealed) {
        if (index >= session.size()) {
            showSessionDone("Карточки просмотрены", session.size(), 0);
            return;
        }
        Word w = session.get(index);
        LinearLayout root = page();
        root.addView(topBar("Карточки", (index + 1) + " / " + session.size()));

        TextView en = bigWord(w.en);
        root.addView(en);
        Button sound = secondaryButton("🔊  Произнести", v -> speak(w.en));
        root.addView(sound);

        TextView ru = bigWord(revealed ? w.ru : "••••••");
        ru.setTextSize(28);
        ru.setPadding(0, dp(18), 0, dp(18));
        root.addView(ru);

        if (!revealed) {
            root.addView(primaryButton("Показать перевод", v -> showCard(session, index, true)));
        } else {
            root.addView(primaryButton("Знаю ✓", v -> showCard(session, index + 1, false)));
            root.addView(secondaryButton("Повторить позже", v -> {
                session.add(w);
                showCard(session, index + 1, false);
            }));
        }
        setContentView(wrap(root));
    }

    private void startTyping(boolean listening) {
        List<Word> session = sessionWords();
        if (session.isEmpty()) {
            Toast.makeText(this, "Словарь пуст! Добавьте слова в Мои слова.", Toast.LENGTH_SHORT).show();
            return;
        }
        onHome = false;
        showTypingQuestion(session, 0, 0, 0, listening);
    }

    private void showTypingQuestion(List<Word> session, int index, int correct, int wrong, boolean listening) {
        if (index >= session.size()) {
            showSessionDone(listening ? "Тренировка на слух завершена" : "Правописание завершено", correct, wrong);
            return;
        }
        Word w = session.get(index);
        LinearLayout root = page();
        root.addView(topBar(listening ? "На слух" : "Правописание", (index + 1) + " / " + session.size()));

        if (listening) {
            TextView prompt = bigWord("🔊");
            prompt.setTextSize(54);
            prompt.setOnClickListener(v -> speak(w.en));
            root.addView(prompt);
            TextView hint = text("Нажми на динамик и напиши английское слово");
            hint.setGravity(Gravity.CENTER);
            hint.setPadding(0, 0, 0, dp(16));
            root.addView(hint);
            root.postDelayed(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    speak(w.en);
                }
            }, 300);
        } else {
            TextView prompt = bigWord(w.ru);
            prompt.setTextSize(32);
            root.addView(prompt);
            TextView hint = text("Напиши по-английски");
            hint.setGravity(Gravity.CENTER);
            hint.setPadding(0, 0, 0, dp(16));
            root.addView(hint);
        }

        EditText answer = new EditText(this);
        answer.setTextSize(20);
        answer.setMaxLines(1);
        answer.setLines(1);
        answer.setHint("English");
        answer.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        answer.setImeOptions(EditorInfo.IME_ACTION_DONE);
        answer.setPadding(dp(16), dp(14), dp(16), dp(14));
        root.addView(answer, matchWrap());

        TextView feedback = text("");
        feedback.setTextSize(18);
        feedback.setGravity(Gravity.CENTER);
        feedback.setPadding(0, dp(12), 0, dp(12));
        root.addView(feedback);

        Button check = primaryButton("Проверить", null);
        root.addView(check);
        answer.setOnEditorActionListener((v, actionId, event) -> {
            check.performClick();
            return true;
        });

        check.setOnClickListener(v -> {
            String given = normalize(answer.getText().toString());
            if (given.isEmpty()) {
                answer.setError("Напиши слово");
                return;
            }
            boolean ok = given.equals(normalize(w.en));
            recordAttempt(ok);
            hideKeyboard(answer);
            answer.setEnabled(false);
            check.setEnabled(false);
            if (ok) {
                feedback.setText("Правильно! ✓");
                feedback.setTextColor(Color.rgb(34, 139, 94));
            } else {
                feedback.setText(String.format(Locale.getDefault(), "Правильный ответ: %s", w.en));
                feedback.setTextColor(Color.rgb(190, 55, 55));
                session.add(w);
            }
            Button next = primaryButton(ok ? "Дальше" : "Запомнить и дальше", vv ->
                    showTypingQuestion(session, index + 1, correct + (ok ? 1 : 0), wrong + (ok ? 0 : 1), listening));
            root.addView(next);
        });

        setContentView(wrap(root));
        answer.requestFocus();
    }

    private void startChoice() {
        List<Word> session = sessionWords();
        if (session.isEmpty()) {
            Toast.makeText(this, "Словарь пуст! Добавьте слова в Мои слова.", Toast.LENGTH_SHORT).show();
            return;
        }
        onHome = false;
        showChoiceQuestion(session, 0, 0, 0);
    }

    private void showChoiceQuestion(List<Word> session, int index, int correct, int wrong) {
        if (index >= session.size()) {
            showSessionDone("Тренировка перевода завершена", correct, wrong);
            return;
        }
        Word w = session.get(index);
        LinearLayout root = page();
        root.addView(topBar("English → русский", (index + 1) + " / " + session.size()));
        root.addView(bigWord(w.en));
        root.addView(secondaryButton("🔊  Произнести", v -> speak(w.en)));

        List<String> options = makeOptions(w);
        TextView feedback = text("");
        feedback.setTextSize(18);
        feedback.setGravity(Gravity.CENTER);
        feedback.setPadding(0, dp(12), 0, dp(8));

        final List<Button> buttons = new ArrayList<>();
        for (String option : options) {
            Button b = choiceButton(option);
            buttons.add(b);
            root.addView(b);
            b.setOnClickListener(v -> {
                boolean ok = option.equals(w.ru);
                recordAttempt(ok);
                for (Button x : buttons) x.setEnabled(false);
                if (ok) {
                    feedback.setText("Правильно! ✓");
                    feedback.setTextColor(Color.rgb(34, 139, 94));
                } else {
                    feedback.setText(String.format(Locale.getDefault(), "Правильный ответ: %s", w.ru));
                    feedback.setTextColor(Color.rgb(190, 55, 55));
                    session.add(w);
                }
                Button next = primaryButton("Дальше", vv ->
                        showChoiceQuestion(session, index + 1, correct + (ok ? 1 : 0), wrong + (ok ? 0 : 1)));
                root.addView(next);
            });
        }
        root.addView(feedback);
        setContentView(wrap(root));
    }

    private void showSessionDone(String heading, int correct, int wrong) {
        onHome = false;
        LinearLayout root = page();
        root.addView(title("🎉 " + heading));
        TextView result;
        if (wrong == 0) {
            result = text("Отличная работа!");
        } else {
            result = text(String.format(Locale.getDefault(),
                    "Правильных ответов: %d\nОшибок: %d\nОшибочные слова уже были добавлены на повторение в этой тренировке.", correct, wrong));
        }
        result.setTextSize(20);
        result.setGravity(Gravity.CENTER);
        result.setPadding(0, dp(24), 0, dp(24));
        root.addView(result);
        root.addView(primaryButton("На главную", v -> showHome()));
        setContentView(wrap(root));
    }

    private void showWords() {
        onHome = false;
        List<Word> words = db.getAllWords();
        LinearLayout root = page();
        root.addView(topBar("Мои слова", words.size() + " слов"));
        root.addView(primaryButton("+ Добавить слово", v -> showAddDialog()));
        root.addView(secondaryButton("Импортировать список", v -> showImportDialog()));

        TextView hint = text("Пользовательские слова можно удалить долгим нажатием.");
        hint.setTextSize(14);
        hint.setPadding(0, dp(10), 0, dp(10));
        root.addView(hint);

        for (Word w : words) {
            TextView row = text(w.en + "   —   " + w.ru + (w.custom ? "  ★" : ""));
            row.setTextSize(16);
            row.setPadding(dp(14), dp(14), dp(14), dp(14));
            row.setBackgroundResource(R.drawable.bg_word_card);
            LinearLayout.LayoutParams lp = matchWrap();
            lp.setMargins(0, 0, 0, dp(8));
            root.addView(row, lp);
            if (w.custom) {
                row.setOnLongClickListener(v -> {
                    new MaterialAlertDialogBuilder(this)
                            .setTitle("Удалить слово?")
                            .setMessage(w.en + " — " + w.ru)
                            .setNegativeButton("Отмена", null)
                            .setPositiveButton("Удалить", (d, which) -> {
                                db.deleteCustomWord(w.id);
                                showWords();
                            }).show();
                    return true;
                });
            }
        }
        setContentView(wrap(root));
    }

    private void showAddDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(22), dp(6), dp(22), 0);
        EditText en = new EditText(this);
        en.setHint("English: apple");
        en.setMaxLines(1);
        en.setLines(1);
        en.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

        EditText ru = new EditText(this);
        ru.setHint("Русский: яблоко");
        ru.setMaxLines(1);
        ru.setLines(1);
        ru.setInputType(InputType.TYPE_CLASS_TEXT);

        box.addView(en, matchWrap());
        box.addView(ru, matchWrap());

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Новое слово")
                .setView(box)
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Добавить", null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String enText = en.getText().toString().trim();
            String ruText = ru.getText().toString().trim();
            if (enText.isEmpty() || ruText.isEmpty()) {
                Toast.makeText(this, "Заполни оба поля", Toast.LENGTH_SHORT).show();
                return;
            }
            if (db.addWord(enText, ruText)) {
                dialog.dismiss();
                showWords();
            } else {
                Toast.makeText(this, "Такое слово уже есть", Toast.LENGTH_SHORT).show();
            }
        }));
        dialog.show();
    }

    private void showImportDialog() {
        EditText input = new EditText(this);
        input.setHint("apple=яблоко\ndog=собака\nbook=книга\ncat - кошка");
        input.setMinLines(8);
        input.setGravity(Gravity.TOP);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        int pad = dp(20);
        LinearLayout box = new LinearLayout(this);
        box.setPadding(pad, 0, pad, 0);
        box.addView(input, matchWrap());

        new MaterialAlertDialogBuilder(this)
                .setTitle("Импорт слов")
                .setMessage("По одной паре на строку. Разделители: =, ;, табуляция или дефис ( - ).")
                .setView(box)
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Импорт", (d, which) -> {
                    int n = db.importWords(input.getText().toString());
                    Toast.makeText(this, "Добавлено слов: " + n, Toast.LENGTH_LONG).show();
                    showWords();
                }).show();
    }

    private void showStats() {
        onHome = false;
        int attempts = prefs.getInt("attempts", 0);
        int correct = prefs.getInt("correct", 0);
        int wrong = attempts - correct;
        int percent = attempts == 0 ? 0 : Math.round(correct * 100f / attempts);

        LinearLayout root = page();
        root.addView(topBar("Статистика", "за всё время"));
        TextView stats = bigWord(attempts == 0 ? "Пока нет ответов" : percent + "%");
        stats.setTextSize(48);
        root.addView(stats);
        TextView details = text(String.format(Locale.getDefault(),
                "Всего ответов: %d\nПравильно: %d\nОшибок: %d\nСлов в словаре: %d",
                attempts, correct, wrong, db.getAllWords().size()));
        details.setTextSize(20);
        details.setGravity(Gravity.CENTER);
        details.setLineSpacing(dp(6), 1f);
        details.setPadding(0, dp(15), 0, dp(25));
        root.addView(details);
        root.addView(secondaryButton("Сбросить статистику", v -> new MaterialAlertDialogBuilder(this)
                .setTitle("Сбросить статистику?")
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Сбросить", (d, w) -> {
                    prefs.edit().clear().apply();
                    showStats();
                }).show()));
        root.addView(primaryButton("На главную", v -> showHome()));
        setContentView(wrap(root));
    }

    private List<Word> sessionWords() {
        List<Word> words = db.getAllWords();
        Collections.shuffle(words);
        if (words.size() > SESSION_SIZE) {
            words = new ArrayList<>(words.subList(0, SESSION_SIZE));
        }
        return new ArrayList<>(words);
    }

    private List<String> makeOptions(Word right) {
        List<Word> all = db.getAllWords();
        Collections.shuffle(all);
        Set<String> set = new HashSet<>();
        set.add(right.ru);
        for (Word w : all) {
            if (set.size() >= 4) break;
            set.add(w.ru);
        }
        List<String> result = new ArrayList<>(set);
        Collections.shuffle(result);
        return result;
    }

    private void speak(String word) {
        if (tts != null && isTtsReady && !isFinishing() && !isDestroyed()) {
            tts.speak(word, TextToSpeech.QUEUE_FLUSH, null, "word_" + System.currentTimeMillis());
        }
    }

    private void recordAttempt(boolean correct) {
        int attempts = prefs.getInt("attempts", 0) + 1;
        int correctCount = prefs.getInt("correct", 0) + (correct ? 1 : 0);
        prefs.edit().putInt("attempts", attempts).putInt("correct", correctCount).apply();
    }

    private String normalize(String s) {
        return s.trim().toLowerCase(Locale.ROOT).replace('’', '\'');
    }

    private void hideKeyboard(View v) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }

    private ScrollView wrap(LinearLayout content) {
        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        sv.setBackgroundColor(Color.WHITE);
        sv.addView(content, new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));
        return sv;
    }

    private LinearLayout page() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(20), dp(24), dp(20), dp(28));
        root.setBackgroundColor(Color.WHITE);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(dp(20), Math.max(dp(24), systemBars.top + dp(12)), dp(20), Math.max(dp(28), systemBars.bottom + dp(16)));
            return insets;
        });
        return root;
    }

    private TextView topBar(String heading, String info) {
        TextView v = text("‹   " + heading + "\n" + info);
        v.setTextSize(18);
        v.setPadding(0, 0, 0, dp(22));
        v.setOnClickListener(x -> showHome());
        return v;
    }

    private TextView title(String s) {
        TextView t = text(s);
        t.setTextSize(32);
        t.setGravity(Gravity.CENTER);
        t.setTextColor(Color.rgb(35, 42, 72));
        t.setPadding(0, dp(8), 0, dp(12));
        return t;
    }

    private TextView bigWord(String s) {
        TextView t = text(s);
        t.setTextSize(38);
        t.setGravity(Gravity.CENTER);
        t.setTextColor(Color.rgb(35, 42, 72));
        t.setPadding(dp(8), dp(38), dp(8), dp(28));
        return t;
    }

    private TextView text(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(16);
        t.setTextColor(Color.rgb(55, 60, 75));
        return t;
    }

    private Button menuButton(String label, View.OnClickListener l) {
        MaterialButton b = (MaterialButton) primaryButton(label, l);
        b.setTextSize(18);
        LinearLayout.LayoutParams lp = matchWrap();
        lp.height = dp(58);
        lp.setMargins(0, 0, 0, dp(10));
        b.setLayoutParams(lp);
        return b;
    }

    private Button primaryButton(String label, View.OnClickListener l) {
        MaterialButton b = new MaterialButton(this);
        b.setText(label);
        b.setTextSize(17);
        b.setAllCaps(false);
        b.setCornerRadius(dp(14));
        b.setTextColor(Color.WHITE);
        b.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(66, 85, 255)));
        if (l != null) b.setOnClickListener(l);
        LinearLayout.LayoutParams lp = matchWrap();
        lp.setMargins(0, dp(6), 0, dp(6));
        b.setLayoutParams(lp);
        return b;
    }

    private Button secondaryButton(String label, View.OnClickListener l) {
        MaterialButton b = new MaterialButton(this, null, com.google.android.material.R.attr.borderlessButtonStyle);
        b.setText(label);
        b.setTextSize(16);
        b.setAllCaps(false);
        b.setCornerRadius(dp(14));
        b.setTextColor(Color.rgb(50, 60, 100));
        b.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(237, 239, 255)));
        if (l != null) b.setOnClickListener(l);
        LinearLayout.LayoutParams lp = matchWrap();
        lp.setMargins(0, dp(5), 0, dp(5));
        b.setLayoutParams(lp);
        return b;
    }

    private Button choiceButton(String label) {
        MaterialButton b = new MaterialButton(this, null, com.google.android.material.R.attr.borderlessButtonStyle);
        b.setText(label);
        b.setTextSize(18);
        b.setAllCaps(false);
        b.setCornerRadius(dp(14));
        b.setTextColor(Color.rgb(45, 50, 70));
        b.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(244, 245, 249)));
        LinearLayout.LayoutParams lp = matchWrap();
        lp.height = dp(58);
        lp.setMargins(0, dp(5), 0, dp(5));
        b.setLayoutParams(lp);
        return b;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
