package com.zhanyage.bindviewannotation;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.zhanyage.bindview.annotation.Bind;
import com.zhanyage.bindview.core.service.BindService;

import java.util.Locale;

public class SimpleAdapter extends BaseAdapter {
  private static final String[] CONTENTS = "The quick brown fox jumps over the lazy dog".split(" ");

  private final LayoutInflater inflater;

  public SimpleAdapter(Context context) {
    inflater = LayoutInflater.from(context);
  }

  @Override public int getCount() {
    return CONTENTS.length;
  }

  @Override public String getItem(int position) {
    return CONTENTS[position];
  }

  @Override public long getItemId(int position) {
    return position;
  }

  @Override public View getView(int position, View view, ViewGroup parent) {
    ViewHolder holder;
    if (view != null) {
      holder = (ViewHolder) view.getTag();
    } else {
      view = inflater.inflate(R.layout.simple_list_item, parent, false);
      holder = new ViewHolder(view);
      view.setTag(holder);
    }

    String word = getItem(position);
    holder.word.setText(String.format(Locale.getDefault(), "Word: %s", word));
    holder.length.setText(String.format(Locale.getDefault(), "Length: %d", word.length()));
    holder.position.setText(String.format(Locale.getDefault(), "Position: %d", position));

    return view;
  }

  static final class ViewHolder {
    @Bind(R.id.word)
    TextView word;
    @Bind(R.id.length)
    TextView length;
    @Bind(R.id.position)
    TextView position;

    ViewHolder(View view) {
      BindService.bind(this, view);
    }
  }
}
