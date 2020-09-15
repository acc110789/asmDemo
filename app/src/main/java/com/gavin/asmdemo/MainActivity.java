package com.gavin.asmdemo;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;

import com.gavin.asmdemo.service.ThirdService;
import com.gavin.asmdemo.service.TwoService;

@Keep
public class MainActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ServiceManager serviceManager = ServiceManager.getInstance();
        Log.e("MainActivity", "contain TwoService:  " + serviceManager.containService(TwoService.class));
        Log.e("MainActivity", "contain ThirdService:  " + serviceManager.containService(ThirdService.class));

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
