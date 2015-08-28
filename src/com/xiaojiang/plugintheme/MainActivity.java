package com.xiaojiang.plugintheme;

import java.io.File;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.lidroid.xutils.HttpUtils;
import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.ResponseInfo;
import com.lidroid.xutils.http.callback.RequestCallBack;
import com.lidroid.xutils.http.client.HttpRequest.HttpMethod;

public class MainActivity extends Activity {

    private PluginProxyContext proxyContext;
    private HttpUtils httpUtils;//网络请求对象
    private View rootView;//根布局
    private Dialog dialog;//选择在线插件主题的对话框
    private ProgressDialog progressDialog;//正在加载提示框

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        proxyContext = new PluginProxyContext(this);
        //加载插件资源
        proxyContext.loadResources(Constant.PLUGINNAME, false);
        //由于使用插件主题方式，所以不能再使用R文件，直接使用布局名就行
        rootView = proxyContext.getLayout("activity_main");
        setContentView(rootView);
        httpUtils = new HttpUtils();
        progressDialog = new ProgressDialog(this);
    }

    
    /**
     * ListView的适配器
     * @author 小姜
     * @time 2015-8-28 上午11:50:09
     */
    class MyAdapter extends BaseAdapter {
        private JSONArray result;
        public MyAdapter(String result) {
            try {
                this.result = new JSONArray(result);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public int getCount() {
            return result.length();
        }

        @Override
        public Object getItem(int position) {
            return position;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView textView = new TextView(MainActivity.this);
            textView.setTextSize(40);
            textView.setPadding(10, 5, 0, 5);
            try {
                final JSONObject plugin = result.getJSONObject(position);
                final String name = plugin.get("name").toString();
                final String url = plugin.get("url").toString();
                textView.setText(name);
                textView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            dialog.dismiss();
                            final String apkName = url.substring(url.lastIndexOf("/") + 1, url.length());
                            final File apkFile = new File(getFilesDir(), apkName);//插件存放路径
                            if (apkFile.exists()) {
                                //如果已经下载过就直接安装
                                installPlugin(apkName);
                                return;
                            }
                            //下载主题插件
                            httpUtils.download(url, apkFile.getAbsolutePath(), new RequestCallBack<File>() {
                                @Override
                                public void onStart() {
                                    progressDialog.setMessage("正在下载主题...");
                                    progressDialog.show();
                                };
                                @Override
                                public void onSuccess(ResponseInfo<File> result) {
                                    if(progressDialog!=null){
                                        progressDialog.dismiss();
                                    }
                                    installPlugin(apkName);
                                }

                                @Override
                                public void onFailure(HttpException arg0, String arg1) {
                                    Toast.makeText(MainActivity.this, "下载主题失败，请检查网络后重试！", 0).show();
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return textView;
        }
    }

    /**
     * 安装主题
     * 
     * @author 小姜
     * @time 2015-8-27 下午2:53:38
     */
    @SuppressLint("NewApi")
    public void installPlugin(String apkName) {
        Constant.PLUGINNAME = apkName;
        onCreate(null);//可以根据这个参数是否为空来判断是切换主题而不是第一次创建activity
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @SuppressLint("NewApi")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        //菜单栏下载主题项点击事件
        if (id == R.id.down_plugintheme) {
            //加载提示框
            progressDialog.setMessage("获取在线主题...");
            progressDialog.show();
            httpUtils.send(HttpMethod.POST, Constant.URL_PLUGINS,
                    new RequestCallBack<String>() {
                        @Override
                        public void onStart() {
                            progressDialog.show();
                            super.onStart();
                        }
                        @Override
                        public void onSuccess(ResponseInfo<String> response) {
                            try {
                                if(progressDialog!=null){
                                    progressDialog.dismiss();
                                }
                                dialog = new Dialog(MainActivity.this);
                                dialog.setTitle("请选择主题");
                                ListView listView = new ListView(MainActivity.this);
                                listView.setAdapter(new MyAdapter(response.result));
                                dialog.setContentView(listView);
                                dialog.show();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onFailure(HttpException arg0, String arg1) {
                            Toast.makeText(MainActivity.this, "网络连接失败，请检查网络后重试！", 0).show();
                        }

                    });
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        System.exit(0);//杀死进程，防止主题缓存
    }

}
