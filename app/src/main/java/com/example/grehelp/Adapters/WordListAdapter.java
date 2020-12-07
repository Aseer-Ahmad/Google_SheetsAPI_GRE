package com.example.grehelp.Adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.grehelp.Models.DataModel;
import com.example.grehelp.R;

import java.util.List;

public class WordListAdapter extends RecyclerView.Adapter<WordListAdapter.WordListViewHolder> {

    private Context context;
    private List<DataModel> list;


    public WordListAdapter(Context context, List<DataModel> list){
        this.context = context;
        this.list = list;

    }

    @NonNull
    @Override
    public WordListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_wordlist, parent, false);
        return new WordListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WordListViewHolder holder, int position) {
        DataModel dataModel = list.get(position);

        holder.textView.setText(dataModel.getWord());
        holder.textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                holder.textView_desc.setText(dataModel.getDesc());
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }


    class WordListViewHolder extends RecyclerView.ViewHolder{

        private TextView textView;
        private TextView textView_desc;

        public WordListViewHolder(@NonNull View itemView) {
            super(itemView);

            textView_desc = (TextView) ((Activity) context).findViewById(R.id.textView_desc);
            textView = itemView.findViewById(R.id.textview_wordlistItem);
        }
    }


}
