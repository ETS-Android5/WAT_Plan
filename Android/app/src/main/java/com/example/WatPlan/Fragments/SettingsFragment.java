package com.example.WatPlan.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.WatPlan.Activities.MainActivity;
import com.example.WatPlan.Adapters.BlockFilter;
import com.example.WatPlan.Adapters.WeekAdapter;
import com.example.WatPlan.Handlers.UpdateHandler;
import com.example.WatPlan.R;
import com.toptoche.searchablespinnerlibrary.SearchableSpinner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class SettingsFragment extends Fragment {
    private String NO_FILTER = "---";
    private int spinnerItem = R.layout.support_simple_spinner_dropdown_item;
    private boolean ready = false;
    private UpdateHandler updateHandler;
    private MainActivity mainActivity;
    private Button pullDataButton;
    private ArrayAdapter<String> groupAdapter, smesterAdapter, subjectAdapter;
    private ArrayList<String> groups, semesters, subjects = new ArrayList<>();
    private Spinner semesterSpinner, subjectSpinner;
    private SearchableSpinner groupSpinner;
    private WeekAdapter weekAdapter;
    private Map<Switch, BlockFilter> switchMap = new HashMap<>();
    private BlockFilter subjectBlockFilter;
    private int[] switchIds = new int[]{R.id.lectureSwitch, R.id.exerciseSwitch, R.id.laboratorySwitch};
    private BlockFilter[] switchFilters = new BlockFilter[]{
            block -> !block.getClassType().equals("w"),
            block -> !block.getClassType().equals("ć"),
            block -> !block.getClassType().equals("L")
    };
    private Map<String, Set<String>> uniqueValues;

    public SettingsFragment(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);


        //check if plan is opened
        for (int i = 0; i < switchIds.length; i++)
            switchMap.put(view.findViewById(switchIds[i]), switchFilters[i]);
        pullDataButton = view.findViewById(R.id.pullDataButton);
        semesterSpinner = view.findViewById(R.id.semesterSpinner);
        groupSpinner = view.findViewById(R.id.groupSpinner);
        subjectSpinner = view.findViewById(R.id.subjectSpinner);

        weekAdapter = mainActivity.getScheduleFragment().getWeekAdapter();
        updateHandler = mainActivity.getUpdateHandler();
        semesters = updateHandler.getAvailableSemesters();
        groups = updateHandler.getAvailableGroups();

        if (!ready) {
            ready = true;
            smesterAdapter = new ArrayAdapter<>(mainActivity, spinnerItem, semesters);
            groupAdapter = new ArrayAdapter<>(mainActivity, spinnerItem, groups);
            subjectAdapter = new ArrayAdapter<>(mainActivity, spinnerItem, this.subjects);

        }
        semesterSpinner.setAdapter(smesterAdapter);
        groupSpinner.setAdapter(groupAdapter);
        subjectSpinner.setAdapter(subjectAdapter);

        addListeners();
        return view;
    }

    private void addListeners() {
        switchMap.forEach((switch_, filter) -> switch_.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {
                    weekAdapter.switchBlockFilter(filter, isChecked);
//                    weekAdapter.notifyDataSetChanged();
                })
        );
        pullDataButton.setOnClickListener(v -> {
            Toast.makeText(mainActivity, "TOASTY", Toast.LENGTH_LONG).show();
        });

        semesterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String semester = semesterSpinner.getSelectedItem().toString();
                updateHandler.setActiveSemester(semester);
                String group = updateHandler.getActiveGroup();
                groups = updateHandler.getAvailableGroups();
                groupAdapter.notifyDataSetChanged();
                updateHandler.changeGroup(semester, group);
                groupSpinner.setSelection(0);
                subjectSpinner.setSelection(0);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        groupSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String group = groupSpinner.getSelectedItem().toString();
                String semester = updateHandler.getActiveSemester();
                updateHandler.changeGroup(semester, group);
                subjectSpinner.setSelection(0);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                System.out.println("NOTHING SELECTED");
            }
        });

        subjectSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                WeekAdapter weekAdapter = mainActivity.getScheduleFragment().getWeekAdapter();
                weekAdapter.switchBlockFilter(subjectBlockFilter, false);

                String subjectFilter = subjectSpinner.getSelectedItem().toString();
                if (subjectFilter.equals(NO_FILTER)) subjectBlockFilter = block -> true;
                else subjectBlockFilter = block -> block.getSubject().equals(subjectFilter);
                weekAdapter.switchBlockFilter(subjectBlockFilter, true);
                weekAdapter.notifyDataSetChanged();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

    }

    public void setUniqueValues(Map<String, Set<String>> uniqueValues) {
        this.uniqueValues = uniqueValues;
        subjects.clear();
        subjects.add(NO_FILTER);
        subjects.addAll(Objects.requireNonNull(uniqueValues.get("subject")));
    }
}
