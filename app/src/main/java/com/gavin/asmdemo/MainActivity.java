package com.gavin.asmdemo;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import com.gavin.asmdemo.service.TwoService;

@Keep
public class MainActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TwoService service = ServiceManager.getInstance().requireService(TwoService.class);
        Log.e("MainActivity", "twoService:  " + service.doBusinessTwo());
    }

    public void toSecond(@NonNull View view) {
        Intent intent = new Intent(this, SecondActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
