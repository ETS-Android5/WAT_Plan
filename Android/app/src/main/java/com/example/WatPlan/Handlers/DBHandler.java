package com.example.WatPlan.Handlers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import com.example.WatPlan.Models.Block;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class DBHandler extends SQLiteOpenHelper {
    private SQLiteDatabase readableDb;
    private SQLiteDatabase writableDb;
    private static final String DATABASE_NAME = "WAT_PLAN";
    private static final String PREFERENCES = "preferences";
    private static final String SEMESTER = "semester";
    private static final String GROUP = "'group'";
    private static final String BLOCK = "block";
    private static final int DATABASE_VERSION = 1;

    public DBHandler(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        readableDb = getReadableDatabase();
        writableDb = getWritableDatabase();
    }


    private void insertGroup(String... args) {
        assert args.length > 1;
        ContentValues values = new ContentValues();
        values.put("semester_name", args[0]);
        values.put("name", args[1]);
        writableDb.insert(GROUP, null, values);
    }

    private void insertSemester(String semesterName) {
        assert semesterName != null;
        ContentValues values = new ContentValues();
        values.put("name", semesterName);
        writableDb.insert(SEMESTER, null, values);
    }

    public void setActiveSemester(String semesterName) {
        ContentValues values = new ContentValues();
        values.put("name", "semester");
        values.put("value", semesterName);
        if (getActiveSemester() == null) writableDb.insert(PREFERENCES, null, values);
        else writableDb.update(PREFERENCES, values, "name='semester'", null);
        setActiveGroup(getGroups(semesterName).get(0));
    }

    public void setActiveGroup(String groupName) {
        ContentValues values = new ContentValues();
        values.put("name", "group");
        values.put("value", groupName);
        if (getActiveGroup() == null) writableDb.insert(PREFERENCES, null, values);
        else writableDb.update(PREFERENCES, values, "name='group'", null);
    }

    void updateBorderDates(String semesterName, String groupName, Pair<String, String> borderDates) {
        String groupId = getGroupId(semesterName, groupName);
        ContentValues values = new ContentValues();
        values.put("first_day", borderDates.first);
        values.put("last_day", borderDates.second);
        writableDb.update(GROUP, values, "id=" + groupId, null);
    }

    void updateGroup(String semesterName, String groupName, Map<Pair<String, String>, Block> blocksMap, String version) {
        ContentValues values = new ContentValues();
        List<String> keyList = Arrays.asList("date", "index", "title", "subject", "teacher",
                "place", "class_type", "class_index");
        String groupId = getGroupId(semesterName, groupName);
        if (groupId == null) {
            insertSemester(semesterName);
            insertGroup(semesterName, groupName);
            groupId = getGroupId(semesterName, groupName);
        }
        boolean exists = planExists(groupId);

        System.out.println("GROUP ID: " + groupId);
        System.out.println("EXISTS: " + exists);

        String finalGroupId = groupId;
        blocksMap.values().forEach(block -> {
            keyList.forEach(key -> values.put("'" + key + "'", block.get(key)));
            if (exists) {
                writableDb.update(BLOCK, values, "block.id=(" +
                        "select block.id from 'group' where " +
                        "'group.id' ='" + finalGroupId + "')", null);
            } else {
                values.put("group_id", finalGroupId);
                writableDb.insert(BLOCK, null, values);
            }
            values.clear();
        });
        values.put("version", version);
        writableDb.update(GROUP, values, "id=" + groupId, null);
        System.out.println("FINISHED UPDATING GROUP");
    }

    private ArrayList<String> getSemesters() {
        ArrayList<String> semesters = new ArrayList<>();
        Cursor cursor = readableDb.rawQuery("select name from semester", null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            semesters.add(cursor.getString(cursor.getColumnIndex("name")));
            cursor.moveToNext();
        }
        cursor.close();
        return semesters;
    }

    private ArrayList<String> getGroups(String semesterName) {
        ArrayList<String> groups = new ArrayList<>();
        Cursor cursor = readableDb.rawQuery("select name from 'group'" +
                " where semester_name='" + semesterName + "'", null);
        cursor.moveToFirst();
        assert cursor.getCount()>0;
        while (!cursor.isAfterLast()) {
            groups.add(cursor.getString(cursor.getColumnIndex("name")));
            cursor.moveToNext();
        }
        cursor.close();
        return groups;
    }

    Map<String, Map<String, String>> getVersionMap() {
        Map<String, Map<String, String>> versionMap = new HashMap<>();
        Cursor cursor = readableDb.rawQuery("select semester_name,name,version from 'group'", null);
        cursor.moveToFirst();

        getSemesters().forEach(semesterName -> versionMap.put(semesterName, new HashMap<>()));
        while (!cursor.isAfterLast()) {
            String semesterName = cursor.getString(cursor.getColumnIndex("semester_name"));
            String groupName = cursor.getString(cursor.getColumnIndex("name"));
            String version = cursor.getString(cursor.getColumnIndex("version"));
            Objects.requireNonNull(versionMap.get(semesterName)).put(groupName, version);
            cursor.moveToNext();
        }
        cursor.close();
        return versionMap;
    }

    String getVersion(String... args) {
        String groupId = getGroupId(args);
        Cursor cursor = readableDb.rawQuery("select version from 'group'" +
                " where id = " + groupId, null);
        if (cursor.getCount() == 0) {
            cursor.close();
            return null;
        }
        cursor.moveToFirst();
        String version = cursor.getString(cursor.getColumnIndex("version"));
        cursor.close();
        return version;
    }

    String getActiveSemester() {
        String semesterName;
        Cursor cursor = readableDb.rawQuery(
                "select value from preferences where name = 'semester'", null);
        cursor.moveToFirst();
        if (cursor.getCount() == 0) semesterName = null;
        else semesterName = cursor.getString(cursor.getColumnIndex("value"));
        cursor.close();
        return semesterName;
    }

    String getActiveGroup() {
        String groupName = null;
        Cursor cursor = readableDb.rawQuery(
                "select value from preferences where name = 'group'", null);
        cursor.moveToFirst();
        if (cursor.getCount() > 0)
            groupName = cursor.getString(cursor.getColumnIndex("value"));
        cursor.close();
        return groupName;
    }

    Map<Pair<String, String>, Block> getGroupBlocks(String... args) {
        String groupId = getGroupId(args);
        Cursor cursor = readableDb.rawQuery("select * from block" +
                " where group_id = " + groupId, null);
        System.out.println("Group " + args[1] + " ID: " + groupId+ "blocks count" + cursor.getCount());
        if (cursor.getCount() == 0)
            throw new AssertionError("Database is missing " + args[1] + " blocks ");
        cursor.moveToFirst();

        Map<Pair<String, String>, Block> blocksMap = new HashMap<>();
        ArrayList<String> columnNames = new ArrayList<>(Arrays.asList(cursor.getColumnNames()));

        while (!cursor.isAfterLast()) {
            Map<String, String> values = new HashMap<>();
            columnNames.forEach(columnName ->
                    values.put(columnName, cursor.getString(cursor.getColumnIndex(columnName))));
            Block block = new Block(values);
            blocksMap.put(new Pair<>(values.get("date"), values.get("index")), block);
            cursor.moveToNext();
        }
        cursor.close();
        return blocksMap;
    }

    Pair<String, String> getBorderDates(String... args) {
        assert args[0] != null && args[1] != null : "invalid semester or group name";
        String groupId = getGroupId(args);
        System.out.println(Arrays.toString(args) + " " + groupId);
        Cursor cursor = readableDb.rawQuery("select first_day,last_day from 'group' where id=" + groupId, null);
        cursor.moveToFirst();
        assert cursor.getCount() > 0 : "invalid group id";
        String firstDay = cursor.getString(cursor.getColumnIndex("first_day"));
        String lastDay = cursor.getString(cursor.getColumnIndex("last_day"));
        if (firstDay == null || lastDay == null) return null;
        cursor.close();
        return new Pair<>(firstDay, lastDay);
    }

    public boolean isEmpty() {
        return getActiveGroup() == null || getActiveSemester() == null;
    }

    public void initialInsert(Map<String, Map<String, String>> versions) {
        assert versions != null;
        ContentValues values = new ContentValues();

        versions.forEach((semester, groups) -> {
            values.put("name", semester);
            writableDb.insert(SEMESTER, null, values);
            values.clear();
            groups.forEach((group, version) -> {
                values.put("semester_name", semester);
                values.put("name", group);
                writableDb.insert(GROUP, null, values);
                values.clear();
            });
        });
    }

    private boolean planExists(String groupId) {
        Cursor cursor = readableDb.rawQuery("select count(block.id) as blocks_count from block" +
                " where block.group_id = " + groupId, null);
        cursor.moveToFirst();
        boolean exists = cursor.getInt(cursor.getColumnIndex("blocks_count")) > 0;
        cursor.close();
        return exists;
    }

    private String getGroupId(String... args) {
        assert args[0] != null && args[1] != null;
        String groupId = null;
        Cursor cursor = readableDb.rawQuery("select * from 'group'" +
                " where semester_name = '" + args[0] + "'" +
                " and name='" + args[1] + "'", null);
        cursor.moveToFirst();
        if (cursor.getCount() > 0)
            groupId = cursor.getString(cursor.getColumnIndex("id"));
        cursor.close();
        return groupId;
    }

    Map<String, Set<String>> getUniqueValues(String... args) {
        Map<String, Set<String>> uniqueValues = new HashMap<>();
        String groupId = getGroupId(args);
        Cursor cursor = readableDb.rawQuery(
                "select * from block where group_id =" + groupId, null);
        cursor.moveToFirst();
        ArrayList<String> columnNames = new ArrayList<>(Arrays.asList(cursor.getColumnNames()));
        columnNames.forEach(columnName -> uniqueValues.put(columnName, new HashSet<>()));
        while (!cursor.isAfterLast()) {
            columnNames.forEach(columnName -> {
                String value = cursor.getString(cursor.getColumnIndex(columnName));
                uniqueValues.get(columnName).add(value);
            });
            cursor.moveToNext();
        }
        System.out.println("GETING UNIQUE VALUES FOR: "+
                args[0] + " " + args[1] + " " + groupId + " " + planExists(groupId));
        cursor.close();
        return uniqueValues;
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table " + PREFERENCES + "(" +
                "name varchar(30) PRIMARY KEY NOT NULL," +
                "value varchar(30) NOT NULL)");

        db.execSQL("create table " + SEMESTER + " (" +
                "name varchar(30) PRIMARY KEY NOT NULL)");

        db.execSQL("CREATE TABLE " + GROUP + " (" +
                "id integer NOT NULL PRIMARY KEY AUTOINCREMENT," +
                "semester_name varchar(30) NOT NULL REFERENCES semester (name) DEFERRABLE INITIALLY DEFERRED," +
                "name varchar(30) NOT NULL," +
                "first_day varchar(30)," +
                "last_day varchar(30)," +
                "version varchar(30) NOT NULL default '-1')");

        db.execSQL("CREATE TABLE " + BLOCK + " (" +
                "id integer NOT NULL PRIMARY KEY AUTOINCREMENT," +
                "group_id integer NOT NULL REFERENCES 'group' (id) DEFERRABLE INITIALLY DEFERRED," +
                "'date' varchar(30) NOT NULL," +
                "'index' integer NOT NULL," +
                "title varchar(100)," +
                "subject varchar(30)," +
                "teacher varchar(30)," +
                "place varchar(30)," +
                "class_type varchar(30)," +
                "class_index varchar(3))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS preferences");
        db.execSQL("DROP TABLE IF EXISTS semester");
        db.execSQL("DROP TABLE IF EXISTS 'group'");
        db.execSQL("DROP TABLE IF EXISTS block");
        onCreate(db);
    }
}
