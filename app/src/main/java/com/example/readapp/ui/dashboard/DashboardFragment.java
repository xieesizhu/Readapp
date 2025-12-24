package com.example.readapp.ui.dashboard;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.readapp.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DashboardFragment extends Fragment {
    private RecyclerView rvBookshelf,rvCategories;
    private BookshelfAdapter bookshelfAdapter;
    private CategoryAdapter categoryAdapter;
    private int selectedCategoryId = -1;
    private MyDBUtils dbHelper;
    private TextView tvEmpty;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_dashboard, container, false);

        // 初始化组件
        rvBookshelf = root.findViewById(R.id.rv_bookshelf);
        rvCategories = root.findViewById(R.id.rv_categories);
        tvEmpty = root.findViewById(R.id.tv_empty);
        dbHelper = new MyDBUtils(requireContext());

        // 设置布局管理器
        rvBookshelf.setLayoutManager(new GridLayoutManager(getContext(), 3));
        rvCategories.setLayoutManager(new LinearLayoutManager(getContext()));

        // 加载数据
        loadCategories();
        loadBookshelfData();

        return root;
    }


    /**
     * 加载分类数据
     */
    private void loadCategories() {
        new Thread(() -> {
            // 从数据库获取所有分类
            List<CategoryBean> categories = dbHelper.getAllCategories();

            // 添加"全部"选项到第一个位置
            List<CategoryBean> fullCategories = new ArrayList<>();
            fullCategories.add(new CategoryBean(-1, "全部"));
            fullCategories.addAll(categories);

            requireActivity().runOnUiThread(() -> {
                // 初始化分类适配器
                categoryAdapter = new CategoryAdapter(fullCategories);
                rvCategories.setAdapter(categoryAdapter);
            });
        }).start();
    }

    /**
     * 加载书架数据（根据当前选中分类）
     */
    private void loadBookshelfData() {
        new Thread(() -> {
            SharedPreferences prefs = requireContext().getSharedPreferences("user_info", Context.MODE_PRIVATE);
            int userId = prefs.getInt("Uid", -1);

            // 根据分类ID获取书籍
            List<BookBean> books;
            if (selectedCategoryId == -1) {
                books = dbHelper.getBookshelfBooks(userId); // 全部书籍
            } else {
                books = dbHelper.getBookshelfBooksByCategory(userId, selectedCategoryId); // 指定分类
            }

            requireActivity().runOnUiThread(() -> updateBookshelfUI(books));
        }).start();
    }
    /**
     * 更新书架UI
     * @param books 书籍列表
     */
    private void updateBookshelfUI(List<BookBean> books) {
        if (books.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            rvBookshelf.setVisibility(View.GONE);
            // 根据分类显示不同提示
            tvEmpty.setText(selectedCategoryId == -1 ?
                    "书架空空如也，快去添加书籍吧~" : "该分类暂无书籍");
        } else {
            tvEmpty.setVisibility(View.GONE);
            rvBookshelf.setVisibility(View.VISIBLE);
            if (bookshelfAdapter == null) {
                bookshelfAdapter = new BookshelfAdapter(books);
                rvBookshelf.setAdapter(bookshelfAdapter);
            } else {
                bookshelfAdapter.updateData(books); // 更新数据
            }
        }
    }

    /**
     * 分类适配器
     */
    class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {
        private List<CategoryBean> categories;      // 分类数据
        private int selectedPosition = 0;       // 当前选中位置

        CategoryAdapter(List<CategoryBean> categories) {
            this.categories = categories;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_category_dash, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            CategoryBean category = categories.get(position);
            holder.tvCategory.setText(category.getCategoryName());

            // 根据选中状态设置字体颜色
            int textColor = position == selectedPosition ?
                    ContextCompat.getColor(requireContext(), R.color.red) : // 红色
                    ContextCompat.getColor(requireContext(), R.color.black); // 默认黑色

            holder.tvCategory.setTextColor(textColor);

            // 点击事件
            holder.itemView.setOnClickListener(v -> {
                // 更新选中状态
                int previousSelected = selectedPosition;
                selectedPosition = holder.getAdapterPosition();
                selectedCategoryId = category.getCategoryId();

                // 刷新前一个选中项和当前选中项
                notifyItemChanged(previousSelected);
                notifyItemChanged(selectedPosition);

                // 刷新书籍列表
                loadBookshelfData();
            });
        }

        @Override
        public int getItemCount() {
            return categories.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvCategory;

            ViewHolder(View itemView) {
                super(itemView);
                tvCategory = itemView.findViewById(R.id.tv_category);
            }
        }
    }

    /**
     * 书架适配器
     */
    class BookshelfAdapter extends RecyclerView.Adapter<BookshelfAdapter.ViewHolder> {
        private List<BookBean> bookList;

        BookshelfAdapter(List<BookBean> bookList) {
            this.bookList = bookList;
        }

        // 更新数据方法
        void updateData(List<BookBean> newBooks) {
            this.bookList = newBooks;
            notifyDataSetChanged(); // 刷新整个列表
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
            BookBean book = bookList.get(position);

            // 加载封面
            Glide.with(DashboardFragment.this)
                    .load(new File(book.getCoverPath()))
                    .placeholder(R.drawable.ic_books)
                    .into(holder.ivCover);

            // 显示标题
            holder.tvTitle.setText((book.getTitle()+"  "+book.getAuthor()+"  "+dbHelper.getCategoryName(book.getCategoryId())));

            // 点击跳转详情
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), BookDetailActivity.class);
                intent.putExtra("book_id", book.getBookId());
                startActivity(intent);
            });

            // 长按移除
            holder.itemView.setOnLongClickListener(v -> {
                new AlertDialog.Builder(requireContext())
                        .setTitle("移除书籍")
                        .setMessage("确定要移除《" + book.getTitle() + "》吗？")
                        .setPositiveButton("确定", (dialog, which) ->
                                removeFromShelf(book.getBookId()))
                        .setNegativeButton("取消", null)
                        .show();
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return bookList.size();
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

    /**
     * 从书架移除书籍（增强：刷新当前分类）
     */
    private void removeFromShelf(int bookId) {
        new Thread(() -> {
            SharedPreferences prefs = requireContext().getSharedPreferences("user_info", Context.MODE_PRIVATE);
            int userId = prefs.getInt("Uid", -1);

            // 执行移除操作
            dbHelper.removeFromShelf(userId, bookId);

            // 重新获取当前分类的书籍
            List<BookBean> newBooks = selectedCategoryId == -1 ?
                    dbHelper.getBookshelfBooks(userId) :
                    dbHelper.getBookshelfBooksByCategory(userId, selectedCategoryId);

            requireActivity().runOnUiThread(() -> {
                if (bookshelfAdapter != null) {
                    bookshelfAdapter.updateData(newBooks);
                    updateBookshelfUI(newBooks); // 更新UI状态
                }
            });
        }).start();
    }


    @Override
    public void onResume() {
        super.onResume();
        // 当从详情页返回时刷新数据
        if (categoryAdapter != null) {
            loadCategories(); // 防止分类数据有更新
        }
        loadBookshelfData();
    }
}