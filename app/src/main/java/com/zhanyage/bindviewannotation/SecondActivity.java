package com.zhanyage.bindviewannotation;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.ListView;
import android.widget.TextView;
import com.zhanyage.bindview.annotation.Bind;
import com.zhanyage.bindview.core.service.BindService;

/**
 * Created by andya on 2019/4/15
 * Describe:
 */
public class SecondActivity extends Activity {

    @Bind(R.id.tv_main)
    TextView tvMain;
    @Bind(R.id.lv_main_list)
    ListView lvMainList;

    private SimpleAdapter simpleAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BindService.bind(this);
        tvMain.setText("哈哈哈哈成功了");
        simpleAdapter = new SimpleAdapter(this);
        lvMainList.setAdapter(simpleAdapter);
        simpleAdapter.notifyDataSetChanged();
    }

}
