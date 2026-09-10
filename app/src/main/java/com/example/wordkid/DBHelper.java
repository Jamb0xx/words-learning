package com.example.wordkid;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DBHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "wordkid.db";
    private static final int DB_VERSION = 1;

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE words (id INTEGER PRIMARY KEY AUTOINCREMENT, en TEXT NOT NULL, ru TEXT NOT NULL, custom INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE UNIQUE INDEX ux_words_en_ru ON words(lower(en), lower(ru))");
        insertStarterWords(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public List<Word> getAllWords() {
        List<Word> result = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT id, en, ru, custom FROM words ORDER BY custom DESC, en COLLATE NOCASE", null)) {
            while (c.moveToNext()) {
                result.add(new Word(c.getLong(0), c.getString(1), c.getString(2), c.getInt(3) == 1));
            }
        }
        return result;
    }

    public boolean addWord(String en, String ru) {
        en = en.trim();
        ru = ru.trim();
        if (en.isEmpty() || ru.isEmpty()) return false;
        ContentValues cv = new ContentValues();
        cv.put("en", en);
        cv.put("ru", ru);
        cv.put("custom", 1);
        return getWritableDatabase().insertWithOnConflict("words", null, cv, SQLiteDatabase.CONFLICT_IGNORE) != -1;
    }

    public int importWords(String text) {
        int added = 0;
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            String[] lines = text.split("\\r?\\n");
            for (String line : lines) {
                String cleaned = line.trim();
                if (cleaned.isEmpty()) continue;
                String[] parts;
                if (cleaned.contains("=")) parts = cleaned.split("=", 2);
                else if (cleaned.contains(";")) parts = cleaned.split(";", 2);
                else if (cleaned.contains("\\t")) parts = cleaned.split("\\t", 2);
                else continue;
                String en = parts[0].trim();
                String ru = parts[1].trim();
                if (en.isEmpty() || ru.isEmpty()) continue;
                ContentValues cv = new ContentValues();
                cv.put("en", en);
                cv.put("ru", ru);
                cv.put("custom", 1);
                if (db.insertWithOnConflict("words", null, cv, SQLiteDatabase.CONFLICT_IGNORE) != -1) added++;
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return added;
    }

    public void deleteCustomWord(long id) {
        getWritableDatabase().delete("words", "id=? AND custom=1", new String[]{String.valueOf(id)});
    }

    private void add(SQLiteDatabase db, String en, String ru) {
        ContentValues cv = new ContentValues();
        cv.put("en", en);
        cv.put("ru", ru);
        cv.put("custom", 0);
        db.insert("words", null, cv);
    }

    private void insertStarterWords(SQLiteDatabase db) {
        String[][] words = new String[][]{
            {"apple","яблоко"},{"banana","банан"},{"orange","апельсин"},{"lemon","лимон"},{"pear","груша"},
            {"grape","виноград"},{"water","вода"},{"milk","молоко"},{"bread","хлеб"},{"cheese","сыр"},
            {"egg","яйцо"},{"meat","мясо"},{"fish","рыба"},{"soup","суп"},{"tea","чай"},
            {"cat","кошка"},{"dog","собака"},{"bird","птица"},{"rabbit","кролик"},{"horse","лошадь"},
            {"cow","корова"},{"mouse","мышь"},{"bear","медведь"},{"fox","лиса"},{"wolf","волк"},
            {"lion","лев"},{"tiger","тигр"},{"elephant","слон"},{"monkey","обезьяна"},{"duck","утка"},
            {"mother","мама"},{"father","папа"},{"sister","сестра"},{"brother","брат"},{"grandmother","бабушка"},
            {"grandfather","дедушка"},{"family","семья"},{"friend","друг"},{"boy","мальчик"},{"girl","девочка"},
            {"teacher","учитель"},{"pupil","ученик"},{"school","школа"},{"classroom","класс"},{"lesson","урок"},
            {"book","книга"},{"notebook","тетрадь"},{"pen","ручка"},{"pencil","карандаш"},{"ruler","линейка"},
            {"bag","сумка"},{"desk","парта"},{"chair","стул"},{"board","доска"},{"picture","картинка"},
            {"house","дом"},{"room","комната"},{"kitchen","кухня"},{"bedroom","спальня"},{"bathroom","ванная"},
            {"door","дверь"},{"window","окно"},{"table","стол"},{"bed","кровать"},{"lamp","лампа"},
            {"garden","сад"},{"street","улица"},{"city","город"},{"park","парк"},{"shop","магазин"},
            {"car","машина"},{"bus","автобус"},{"train","поезд"},{"bike","велосипед"},{"plane","самолёт"},
            {"red","красный"},{"blue","синий"},{"green","зелёный"},{"yellow","жёлтый"},{"black","чёрный"},
            {"white","белый"},{"brown","коричневый"},{"pink","розовый"},{"grey","серый"},{"purple","фиолетовый"},
            {"one","один"},{"two","два"},{"three","три"},{"four","четыре"},{"five","пять"},
            {"six","шесть"},{"seven","семь"},{"eight","восемь"},{"nine","девять"},{"ten","десять"},
            {"Monday","понедельник"},{"Tuesday","вторник"},{"Wednesday","среда"},{"Thursday","четверг"},{"Friday","пятница"},
            {"Saturday","суббота"},{"Sunday","воскресенье"},{"morning","утро"},{"afternoon","день"},{"evening","вечер"},
            {"night","ночь"},{"today","сегодня"},{"tomorrow","завтра"},{"yesterday","вчера"},{"week","неделя"},
            {"spring","весна"},{"summer","лето"},{"autumn","осень"},{"winter","зима"},{"sun","солнце"},
            {"rain","дождь"},{"snow","снег"},{"wind","ветер"},{"sky","небо"},{"cloud","облако"},
            {"tree","дерево"},{"flower","цветок"},{"grass","трава"},{"river","река"},{"sea","море"},
            {"head","голова"},{"face","лицо"},{"eye","глаз"},{"ear","ухо"},{"nose","нос"},
            {"mouth","рот"},{"hand","кисть руки"},{"arm","рука"},{"leg","нога"},{"foot","ступня"},
            {"shirt","рубашка"},{"T-shirt","футболка"},{"dress","платье"},{"skirt","юбка"},{"trousers","брюки"},
            {"shoes","обувь"},{"hat","шапка"},{"coat","пальто"},{"sock","носок"},{"jacket","куртка"},
            {"big","большой"},{"small","маленький"},{"good","хороший"},{"bad","плохой"},{"happy","счастливый"},
            {"sad","грустный"},{"hot","горячий"},{"cold","холодный"},{"new","новый"},{"old","старый"},
            {"long","длинный"},{"short","короткий"},{"beautiful","красивый"},{"funny","смешной"},{"kind","добрый"},
            {"go","идти"},{"come","приходить"},{"run","бежать"},{"walk","гулять"},{"jump","прыгать"},
            {"sit","сидеть"},{"stand","стоять"},{"read","читать"},{"write","писать"},{"speak","говорить"},
            {"listen","слушать"},{"look","смотреть"},{"see","видеть"},{"eat","есть"},{"drink","пить"},
            {"sleep","спать"},{"play","играть"},{"sing","петь"},{"dance","танцевать"},{"swim","плавать"},
            {"open","открывать"},{"close","закрывать"},{"give","давать"},{"take","брать"},{"make","делать"},
            {"like","нравиться"},{"love","любить"},{"want","хотеть"},{"have","иметь"},{"know","знать"},
            {"I","я"},{"you","ты"},{"he","он"},{"she","она"},{"we","мы"},
            {"they","они"},{"this","это"},{"that","то"},{"here","здесь"},{"there","там"}
        };
        for (String[] w : words) add(db, w[0], w[1]);
    }
}
