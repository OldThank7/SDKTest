package com.example.lbttest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    public LocationClient mLocationClient;
    private TextView positionText;
    private MapView mapView;
    private BaiduMap baiduMap;
    private boolean isFirstLocate = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_main);

        mLocationClient = new LocationClient(getApplicationContext());
        mLocationClient.registerLocationListener(new MyLocationListener());
        positionText = (TextView) findViewById(R.id.text_view);
        mapView = (MapView) findViewById(R.id.bmapView);
        baiduMap = mapView.getMap();
        baiduMap.setMyLocationEnabled(true);

        List<String> permissionList = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.READ_PHONE_STATE);
        }
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (!permissionList.isEmpty()){
            String [] permissions = permissionList.toArray(new String[permissionList.size()]);
            ActivityCompat.requestPermissions(MainActivity.this,permissions,1);
        }else {
            requestLocation();
            Toast.makeText(this, "定位成功", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLocationClient.stop();
        mapView.onDestroy();
        baiduMap.setMyLocationEnabled(false);
    }

    private void requestLocation(){
        initLocation();
        mLocationClient.start();
    }

    private void initLocation() {
        LocationClientOption option = new LocationClientOption();
        option.setCoorType("bd09ll");// 坐标类型
        option.setIsNeedAddress(true);// 可选，设置是否需要地址信息，默认不需要
        option.setOpenGps(true);// 打开Gps
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        option.setScanSpan(1000);// 1000毫秒定位一次
        option.setIsNeedLocationPoiList(true);// 可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到
        mLocationClient.setLocOption(option);
    }

    public class MyLocationListener extends BDAbstractLocationListener{
        @Override
        public void onReceiveLocation(final BDLocation location) {
            if (location.getLocType() == BDLocation.TypeGpsLocation || location.getLocType() == BDLocation.TypeNetWorkLocation){
                initLocation();
                navigateTo(location);
            }
        }
    }

    private void navigateTo(BDLocation location){
        if (isFirstLocate) {
            LatLng ll = new LatLng(location.getLatitude(), location.getLongitude());
            MapStatusUpdate update = MapStatusUpdateFactory.newLatLng(ll);
            baiduMap.animateMapStatus(update);
            update = MapStatusUpdateFactory.zoomTo(16f);
            baiduMap.animateMapStatus(update);
            isFirstLocate = false;
        }
        MyLocationData.Builder builder = new MyLocationData.Builder();
        builder.latitude(location.getLatitude());
        builder.longitude(location.getLongitude());
        MyLocationData locationData = builder.build();
        baiduMap.setMyLocationData(locationData);

        /**
         * 创建StringBuilder实例
         * 将获取到的数据都存放在currentposition里面
         * */
        StringBuilder currentPosition = new StringBuilder();
        currentPosition
                .append("纬度:").append(location.getLatitude()).append("    ")//获取纬度信息
                .append("经度:").append(location.getLongitude()).append("\n");//获取经度信息
        /**
         * 中国地理位置
         * */
        currentPosition
                .append(location.getCountry()).append("  ")
                .append(location.getProvince()).append("  ")
                .append(location.getCity()).append("  ")
                .append(location.getDistrict()).append("  ")
                .append(location.getStreet()).append("\n");
        currentPosition.append("精度:").append(location.getRadius());
        currentPosition.append("定位方式:\t");

        /**
         * 判断使用GPS还是网络定位
         * 网络定位工作原理：根据手机当前的网络附近的三个基站进行测速，
         * 以计算出手机和每个基站之间的距离，再通过三角定位确定去一个大概的位置
         * GPS定位工作原理：手机内置的GPS硬件直接和卫星交互老获取当前的经纬度信息
         * */
        if (location.getLocType() == BDLocation.TypeGpsLocation){
            currentPosition.append("GPS");
        }else if(location.getLocType() == BDLocation.TypeNetWorkLocation){
            currentPosition.append("网络");
        }
        positionText.setText(currentPosition);
    }

    //基于权限
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case 1:
                if (grantResults.length > 0){
                    /**
                     * 循环将每个权限进行判断，如果有任何一个权限被拒绝，那就直接调用finish()
                     * */
                    for (int result:grantResults){
                        if (result != PackageManager.PERMISSION_DENIED){
                            Toast.makeText(this, "必须统一说有权限才能使用本程序", Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                    }
                    requestLocation();
                }else{
                    Toast.makeText(this, "发生未知错误", Toast.LENGTH_SHORT).show();
                }
            default:
                break;
        }
    }
}
