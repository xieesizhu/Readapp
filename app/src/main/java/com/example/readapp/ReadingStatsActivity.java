package com.example.readapp;

import android.graphics.Color;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.List;

public class ReadingStatsActivity extends AppCompatActivity {
    private MyDBUtils dbHelper;
    private PieChart pcCategory;
    private BarChart bcTopBooks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reading_stats);

        dbHelper = new MyDBUtils(this);
        pcCategory = findViewById(R.id.pc_category);
        bcTopBooks = findViewById(R.id.bc_top_books);

        setupCharts();
        loadCategoryStats();
        loadBookStats();
    }

    private void setupCharts() {
        // 分类饼图设置
        pcCategory.setUsePercentValues(true);
        pcCategory.getDescription().setEnabled(false);
        pcCategory.setEntryLabelColor(Color.BLACK);

        // 书籍柱状图设置
        bcTopBooks.getDescription().setEnabled(false);
        bcTopBooks.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        bcTopBooks.getAxisLeft().setGranularity(1f);
        bcTopBooks.getAxisRight().setEnabled(false);
    }

    private void loadCategoryStats() {
        new Thread(() -> {
            List<CategoryStat> stats = dbHelper.getCategoryStats();
            runOnUiThread(() -> updateCategoryChart(stats));
        }).start();
    }

    private void loadBookStats() {
        new Thread(() -> {
            List<BookStat> stats = dbHelper.getTopBookStats(5);
            runOnUiThread(() -> updateBookChart(stats));
        }).start();
    }

    private void updateCategoryChart(List<CategoryStat> stats) {
        ArrayList<PieEntry> entries = new ArrayList<>();
        for (CategoryStat stat : stats) {
            entries.add(new PieEntry(stat.totalViews, stat.categoryName));
        }

        PieDataSet dataSet = new PieDataSet(entries, "分类阅读量");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextSize(12f);

        PieData data = new PieData(dataSet);
        pcCategory.setData(data);
        pcCategory.invalidate();
    }

    private void updateBookChart(List<BookStat> stats) {
        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        for (int i = 0; i < stats.size(); i++) {
            entries.add(new BarEntry(i, stats.get(i).viewCount));
            labels.add(stats.get(i).title);
        }

        BarDataSet dataSet = new BarDataSet(entries, "书籍阅读量");
        dataSet.setColors(ColorTemplate.VORDIPLOM_COLORS);

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.9f);

        bcTopBooks.setData(data);
        bcTopBooks.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        bcTopBooks.invalidate();
    }
}
