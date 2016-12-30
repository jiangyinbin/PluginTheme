package com.xiaojiang.plugintheme;


import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;

/**
 * 可直接加载插件布局和资源的自定义Context包装类
 *
 * @author 小姜
 * @time 2015-4-16 上午11:03:47
 */
public class PluginProxyContext extends ContextWrapper {

    private Context context;
    private AssetManager mAssetManager = null;
    private Resources mResources = null;
    private LayoutInflater mLayoutInflater = null;
    private Theme mTheme = null;
    private String packageName = null;

    // *****************资源ID类型******************* //
    public static final String LAYOUT = "layout";
    public static final String ID = "id";
    public static final String ANIM = "anim";
    public static final String DRAWABLE = "drawable";
    public static final String MIPMAP = "mipmap";
    public static final String STYLE = "style";
    public static final String STRING = "string";
    public static final String COLOR = "color";
    public static final String DIMEN = "dimen";

    public PluginProxyContext(Context base) {
        super(base);
        this.context = base;
    }

    /**
     * 加载插件中的资源
     *
     * @param resPluginName 资源插件的apk的名称
     * @author 小姜
     * @time 2015-4-16 上午11:31:36
     */
    public boolean loadResources(String resPluginName) {
        try {
            File outFile = new File(resPluginName);
            if (!outFile.exists()) {
                return false;
            }
            AssetManager assetManager = AssetManager.class.newInstance();
            Method addAssetPath = assetManager.getClass().getMethod("addAssetPath", String.class);
            addAssetPath.invoke(assetManager, outFile.getPath());
            mAssetManager = assetManager;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        Resources superRes = super.getResources();
        mResources = new Resources(mAssetManager, superRes.getDisplayMetrics(), superRes.getConfiguration());
        PackageManager pm = context.getPackageManager();
        PackageInfo info = pm.getPackageArchiveInfo(resPluginName, PackageManager.GET_ACTIVITIES);

        if (info != null) {
            this.packageName = info.packageName;
        }
        Log.d("Theme", "themeApk packageName;" + packageName);
        getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        return true;
    }

    /**
     * 获取插件资源对应的id
     *
     * @param type
     * @param name
     * @return
     * @author 小姜
     * @time 2015-4-16 上午11:31:56
     */
    public int getIdentifier(String type, String name) {
        return mResources.getIdentifier(name, type, packageName);
    }

    public int getId(String name) {
        return mResources.getIdentifier(name, ID, packageName);
    }

    /**
     * 获取插件中的layout布局
     *
     * @param name
     * @return
     * @author 小姜
     * @time 2015-4-16 上午11:32:12
     */
    public View getLayout(String name) {
        int id = getIdentifier(LAYOUT, name);
        Log.d("Plugin", "getLayout:" + name + " id:" + id);
        if (id > 0) {
            return mLayoutInflater.inflate(id, null);
        } else {
            return null;
        }
    }

    public Animation getAnimation(String name) {
        int id = getIdentifier(ANIM, name);
        if (id > 0) {
            XmlResourceParser xmlResourceParser = getResources().getAnimation(id);
            try {
                return createAnimationFromXml(context, xmlResourceParser);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }

    public String getString(String name) {
        int id = getIdentifier(STRING, name);
        if (id > 0) {
            return mResources.getString(id);
        } else {
            return null;
        }
    }

    public int getColor(String name) {
        int id = getIdentifier(COLOR, name);
        if (id > 0) {
            return mResources.getColor(id);
        } else {
            return -1;
        }
    }

    public Drawable getMipmap(String name) {
        int id = getIdentifier(MIPMAP, name);
        if (id > 0) {
            return mResources.getDrawable(id);
        } else {
            return null;
        }
    }

    public Drawable getDrawable(String name) {
        int id = getIdentifier(DRAWABLE, name);
        if (id > 0) {
            return mResources.getDrawable(id);
        } else {
            return null;
        }
    }

    public int getStyle(String name) {
        int id = getIdentifier(STYLE, name);
        if (id > 0) {
            return id;
        } else {
            return -1;
        }
    }

    public float getDimen(String name) {
        int id = getIdentifier(DIMEN, name);
        if (id > 0) {
            return mResources.getDimension(getIdentifier(DIMEN, name));
        } else {
            return -1;
        }
    }

    /**
     * 创建一个当前类的布局加载器，用于专门加载插件资源
     */
    @Override
    public Object getSystemService(String name) {
        if (LAYOUT_INFLATER_SERVICE.equals(name)) {
            if (mLayoutInflater == null) {
                try {
                    Class<?> cls = Class
                            .forName("com.android.internal.policy.PolicyManager");
                    Method m = cls.getMethod("makeNewLayoutInflater",
                            Context.class);
                    //传入当前PluginProxyContext类实例，创建一个布局加载器
                    mLayoutInflater = (LayoutInflater) m.invoke(null, this);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            } else {
                return mLayoutInflater;
            }
        }
        return super.getSystemService(name);
    }

    @Override
    public AssetManager getAssets() {
        return mAssetManager;
    }

    @Override
    public Resources getResources() {
        return mResources;
    }

    @Override
    public ClassLoader getClassLoader() {
        return context.getClassLoader();
    }

    @Override
    public Theme getTheme() {
        if (mTheme == null) {
            mTheme = mResources.newTheme();
            mTheme.applyStyle(android.R.style.Theme_Light, true);
        }
        return mTheme;
    }

    @Override
    public String getPackageName() {
        return packageName;
    }


    //从XML中创建动画对象
    private Animation createAnimationFromXml(Context c, XmlPullParser parser)
            throws XmlPullParserException, IOException {

        return createAnimationFromXml(c, parser, null, Xml.asAttributeSet(parser));
    }

    private Animation createAnimationFromXml(Context c, XmlPullParser parser, AnimationSet parent, AttributeSet attrs) throws XmlPullParserException, IOException {
        Animation anim = null;
        // Make sure we are on a start tag.
        int type;
        int depth = parser.getDepth();
        while (((type = parser.next()) != XmlPullParser.END_TAG || parser.getDepth() > depth)
                && type != XmlPullParser.END_DOCUMENT) {

            if (type != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals("set")) {
                anim = new AnimationSet(c, attrs);
                createAnimationFromXml(c, parser, (AnimationSet) anim, attrs);
            } else if (name.equals("alpha")) {
                anim = new AlphaAnimation(c, attrs);
            } else if (name.equals("scale")) {
                anim = new ScaleAnimation(c, attrs);
            } else if (name.equals("rotate")) {
                anim = new RotateAnimation(c, attrs);
            } else if (name.equals("translate")) {
                anim = new TranslateAnimation(c, attrs);
            } else {
                throw new RuntimeException("Unknown animation name: " + parser.getName());
            }

            if (parent != null) {
                parent.addAnimation(anim);
            }
        }
        return anim;
    }
}