package com.example.readapp.ui.home;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.readapp.*;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class HomeFragment extends Fragment {
    private RecyclerView rvFeatured, rvRecommend;
    private MyDBUtils dbHelper;
    private RecyclerView rvRandom;
    private Handler autoScrollHandler = new Handler();
    private int currentFeaturedPosition = 0;
    private Runnable autoScrollRunnable = new Runnable() {
        @Override
        public void run() {
            if (rvFeatured.getAdapter() != null && rvFeatured.getAdapter().getItemCount() > 0) {
                currentFeaturedPosition++;
                if (currentFeaturedPosition >= rvFeatured.getAdapter().getItemCount()) {
                    currentFeaturedPosition = 0;
                }
                rvFeatured.smoothScrollToPosition(currentFeaturedPosition);
                autoScrollHandler.postDelayed(this, 5000);
            }
        }
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        // 初始化组件
        rvFeatured = root.findViewById(R.id.rv_featured);
        rvRecommend = root.findViewById(R.id.rv_recommend);
        rvRandom = root.findViewById(R.id.rv_random);
        rvRandom.setLayoutManager(new GridLayoutManager(getContext(), 2));
        dbHelper = new MyDBUtils(requireContext());

        // 设置布局管理器
        rvFeatured.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvRecommend.setLayoutManager(new GridLayoutManager(getContext(), 2));

        // 设置功能按钮
        root.findViewById(R.id.btn_search).setOnClickListener(v ->
                startActivity(new Intent(getActivity(), SearchActivity.class)));
        root.findViewById(R.id.btn_category).setOnClickListener(v ->
                startActivity(new Intent(getActivity(), CategoryActivity.class)));

        // 加载数据
        loadFeaturedBooks();
        loadRecommendBooks();
        loadRandomBooks();

        return root;
    }

    @Override
    public void onPause() {
        super.onPause();
        autoScrollHandler.removeCallbacks(autoScrollRunnable);
    }


    /**
     * 加载精选好书（推荐书籍）
     */
    private void loadFeaturedBooks() {
        new Thread(() -> {
            List<BookBean> books = dbHelper.getRecommendedBooks(3); // 最多取3本
            if (books.size() < 3) {
                books.addAll(dbHelper.getRandomBooks(3 - books.size()));
            }
            requireActivity().runOnUiThread(() ->
                    rvFeatured.setAdapter(new BookAdapter(books)));
                    autoScrollHandler.postDelayed(autoScrollRunnable, 5000);
        }).start();
    }

    /**
     * 加载智能推荐书籍
     */
    private void loadRecommendBooks() {
        new Thread(() -> {
            SharedPreferences prefs = requireContext().getSharedPreferences("user_info", Context.MODE_PRIVATE);
            int userId = prefs.getInt("Uid", -1);

            // 直接获取4本书籍
            List<BookBean> books = dbHelper.getSmartRecommendations(userId, 4);

            requireActivity().runOnUiThread(() -> {
                // 使用网格布局
                rvRecommend.setLayoutManager(new GridLayoutManager(getContext(), 2));
                BookAdapter adapter = new BookAdapter(books);
                rvRecommend.setAdapter(adapter);

                // 测量并设置固定高度（解决嵌套滚动问题）
                rvRecommend.post(() -> {
                    ViewGroup.LayoutParams params = rvRecommend.getLayoutParams();
                    params.height = calculateRecyclerViewHeight(adapter.getItemCount());
                    rvRecommend.setLayoutParams(params);
                });
            });
        }).start();
    }

    /**
     * 根据项目数量计算列表高度
     */
    private int calculateRecyclerViewHeight(int itemCount) {
        int rows = (int) Math.ceil(itemCount / 2.0); // 每行2个
        int itemHeight = dpToPx(160); // 每个项目高度（根据实际调整）
        int spacing = dpToPx(16); // 间距

        return (itemHeight + spacing) * rows;
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    /**
     * 加载随机推荐书籍
     */
    private void loadRandomBooks() {
        new Thread(() -> {
            List<BookBean> randomBooks = dbHelper.getRandomBooks(6);
            requireActivity().runOnUiThread(() ->
                    rvRandom.setAdapter(new BookAdapter(randomBooks))
            );
        }).start();
    }

    /**
     * 书籍适配器（完整实现）
     */
    class BookAdapter extends RecyclerView.Adapter<BookAdapter.ViewHolder> {
        private List<BookBean> books;

        BookAdapter(List<BookBean> books) {
            this.books = books;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_book_grid, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            BookBean book = books.get(position);
            holder.tvTitle.setText(book.getTitle()+"  "+book.getAuthor()+"  "+dbHelper.getCategoryName(book.getCategoryId()) + "  点击量:" + book.getViewCount());
            //holder.tvTitle.setText(book.getTitle()+"  "+book.getAuthor()+"  "+dbHelper.getCategoryName(book.getCategoryId()));
            Glide.with(HomeFragment.this)
                    .load(new File(book.getCoverPath()))
                    .placeholder(R.drawable.ic_books)
                    .into(holder.ivCover);

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), BookDetailActivity.class);
                intent.putExtra("book_id", book.getBookId());
                startActivity(intent);
            });
        }

        public void updateData(List<BookBean> newBooks) {
            this.books = newBooks;
            notifyDataSetChanged();
        }

        @Override
        public int getItemCount() {
            return books.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivCover;
            TextView tvTitle;

            ViewHolder(View itemView) {
                super(itemView);
                ivCover = itemView.findViewById(R.id.iv_cover);
                tvTitle = itemView.findViewById(R.id.tv_title);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        autoScrollHandler.postDelayed(autoScrollRunnable, 5000);
        refreshRandomBooks();
        refreshRecommendBooks();
    }

    //加载随机推荐
    private void refreshRandomBooks() {
        new Thread(() -> {
            List<BookBean> books = dbHelper.getRandomBooks(6);
            requireActivity().runOnUiThread(() -> {
                if (rvRandom.getAdapter() != null) {
                    ((BookAdapter)rvRandom.getAdapter()).updateData(books);
                }
            });
        }).start();
    }


    //加载智能推荐
    private void refreshRecommendBooks() {
        new Thread(() -> {
            SharedPreferences prefs = requireContext().getSharedPreferences("user_info", Context.MODE_PRIVATE);
            int userId = prefs.getInt("Uid", -1);
            List<BookBean> books = dbHelper.getSmartRecommendations(userId, 4);

            requireActivity().runOnUiThread(() -> {
                // 直接使用现有适配器更新数据
                if (rvRecommend.getAdapter() != null) {
                    ((BookAdapter)rvRecommend.getAdapter()).updateData(books);
                } else {
                    rvRecommend.setAdapter(new BookAdapter(books));
                }
            });
        }).start();
    }


}