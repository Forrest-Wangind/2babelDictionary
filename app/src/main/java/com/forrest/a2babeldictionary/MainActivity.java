package com.forrest.a2babeldictionary;

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;

import com.baidu.speech.VoiceRecognitionService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements RecognitionListener {

    private static final int EVENT_ERROR = 11;
    private static final String TAG = MainActivity.class.getName();
    private EditText mOriginLanguage;
    private EditText mTransferLanuage;
    private Button mSetting;
    private Button mStartVoice;
    private SpeechRecognizer mSpeechRecognizer;
    private View mSpeechTips;
    private View mSpeechWave;
    private long speechEndTime = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mOriginLanguage = (EditText) findViewById(R.id.origin_language);
        mTransferLanuage = (EditText) findViewById(R.id.transfer_language);
        mSetting = (Button) findViewById(R.id.btn_voice_setting);
        mSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent("com.baidu.speech.asr.demo.setting");
                startActivity(intent);
            }
        });

        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this, new ComponentName(this, VoiceRecognitionService.class));
        mSpeechRecognizer.setRecognitionListener(this);

        mSpeechTips = View.inflate(this, R.layout.bd_asr_popup_speech, null);
        mSpeechWave = mSpeechTips.findViewById(R.id.wave);
        mSpeechTips.setVisibility(View.GONE);
        addContentView(mSpeechTips, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        mStartVoice = (Button) findViewById(R.id.btn_start_voice);
        mStartVoice.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        mSpeechTips.setVisibility(View.VISIBLE);
                        mSpeechRecognizer.cancel();
                        Intent intent = new Intent();
                        bindParams(intent);
                        intent.putExtra("vad", "touch");
                        mOriginLanguage.setText("");
                        mTransferLanuage.setText("");
                        mSpeechRecognizer.startListening(intent);
                        return true;
                    case MotionEvent.ACTION_UP:
                        mSpeechRecognizer.stopListening();
                        mSpeechTips.setVisibility(View.GONE);
                        break;
                }

                return false;
            }
        });
    }

    public void bindParams(Intent intent) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        if (sp.getBoolean("tips_sound", true)) {
            intent.putExtra(Constant.EXTRA_SOUND_START, R.raw.bdspeech_recognition_start);
            intent.putExtra(Constant.EXTRA_SOUND_END, R.raw.bdspeech_speech_end);
            intent.putExtra(Constant.EXTRA_SOUND_SUCCESS, R.raw.bdspeech_recognition_success);
            intent.putExtra(Constant.EXTRA_SOUND_ERROR, R.raw.bdspeech_recognition_error);
            intent.putExtra(Constant.EXTRA_SOUND_CANCEL, R.raw.bdspeech_recognition_cancel);
        }
        if (sp.contains(Constant.EXTRA_INFILE)) {
            String tmp = sp.getString(Constant.EXTRA_INFILE, "").replaceAll(",.*", "").trim();
            intent.putExtra(Constant.EXTRA_INFILE, tmp);
        }
        if (sp.getBoolean(Constant.EXTRA_OUTFILE, false)) {
            intent.putExtra(Constant.EXTRA_OUTFILE, "sdcard/outfile.pcm");
        }
        if (sp.contains(Constant.EXTRA_SAMPLE)) {
            String tmp = sp.getString(Constant.EXTRA_SAMPLE, "").replaceAll(",.*", "").trim();
            if (null != tmp && !"".equals(tmp)) {
                intent.putExtra(Constant.EXTRA_SAMPLE, Integer.parseInt(tmp));
            }
        }
        if (sp.contains(Constant.EXTRA_LANGUAGE)) {
            String tmp = sp.getString(Constant.EXTRA_LANGUAGE, "").replaceAll(",.*", "").trim();
            if (null != tmp && !"".equals(tmp)) {
                intent.putExtra(Constant.EXTRA_LANGUAGE, tmp);
            }
        }
        if (sp.contains(Constant.EXTRA_NLU)) {
            String tmp = sp.getString(Constant.EXTRA_NLU, "").replaceAll(",.*", "").trim();
            if (null != tmp && !"".equals(tmp)) {
                intent.putExtra(Constant.EXTRA_NLU, tmp);
            }
        }

        if (sp.contains(Constant.EXTRA_VAD)) {
            String tmp = sp.getString(Constant.EXTRA_VAD, "").replaceAll(",.*", "").trim();
            if (null != tmp && !"".equals(tmp)) {
                intent.putExtra(Constant.EXTRA_VAD, tmp);
            }
        }
        String prop = null;
        if (sp.contains(Constant.EXTRA_PROP)) {
            String tmp = sp.getString(Constant.EXTRA_PROP, "").replaceAll(",.*", "").trim();
            if (null != tmp && !"".equals(tmp)) {
                intent.putExtra(Constant.EXTRA_PROP, Integer.parseInt(tmp));
                prop = tmp;
            }
        }
        // offline asr
        {
            intent.putExtra(Constant.EXTRA_OFFLINE_ASR_BASE_FILE_PATH, "/sdcard/easr/s_1");
            intent.putExtra(Constant.EXTRA_LICENSE_FILE_PATH, "/sdcard/easr/license-tmp-20150530.txt");
            if (null != prop) {
                int propInt = Integer.parseInt(prop);
                if (propInt == 10060) {
                    intent.putExtra(Constant.EXTRA_OFFLINE_LM_RES_FILE_PATH, "/sdcard/easr/s_2_Navi");
                } else if (propInt == 20000) {
                    intent.putExtra(Constant.EXTRA_OFFLINE_LM_RES_FILE_PATH, "/sdcard/easr/s_2_InputMethod");
                }
            }
            intent.putExtra(Constant.EXTRA_OFFLINE_SLOT_DATA, buildTestSlotData());
        }
    }

    private String buildTestSlotData() {
        JSONObject slotData = new JSONObject();
        JSONArray name = new JSONArray().put("李涌泉").put("郭下纶");
        JSONArray song = new JSONArray().put("七里香").put("发如雪");
        JSONArray artist = new JSONArray().put("周杰伦").put("李世龙");
        JSONArray app = new JSONArray().put("手机百度").put("百度地图");
        JSONArray usercommand = new JSONArray().put("关灯").put("开门");
        try {
            slotData.put("name", name);
            slotData.put("song", song);
            slotData.put("artist", artist);
            slotData.put("app", app);
            slotData.put("usercommand", usercommand);
        } catch (JSONException e) {

        }

        return slotData.toString();
    }

    @Override
    public void onReadyForSpeech(Bundle params) {

    }

    @Override
    public void onBeginningOfSpeech() {

    }

    //引擎将对每一帧语音进行回调一次改方法
    @Override
    public void onRmsChanged(float rmsdB) {
        final int VTAG = 0xFF00AA01;
        Integer rawHeight = (Integer) mSpeechWave.getTag(VTAG);
        if (rawHeight == null) {
            rawHeight = mSpeechWave.getLayoutParams().height;
            mSpeechWave.setTag(VTAG, rawHeight);
        }

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mSpeechWave.getLayoutParams();
        params.height = (int) (rawHeight * rmsdB * 0.01);
        params.height = Math.max(params.height, mSpeechWave.getMeasuredWidth());
        mSpeechWave.setLayoutParams(params);
    }

    @Override
    public void onBufferReceived(byte[] buffer) {

    }

    @Override
    public void onEndOfSpeech() {

    }

    @Override
    public void onError(int error) {
        StringBuilder sb = new StringBuilder();
        switch (error) {
            case SpeechRecognizer.ERROR_AUDIO:
                sb.append("音频问题");
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                sb.append("没有语音输入");
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                sb.append("其它客户端错误");
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                sb.append("权限不足");
                break;
            case SpeechRecognizer.ERROR_NETWORK:
                sb.append("网络问题");
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                sb.append("没有匹配的识别结果");
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                sb.append("引擎忙");
                break;
            case SpeechRecognizer.ERROR_SERVER:
                sb.append("服务端错误");
                break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                sb.append("连接超时");
                break;
        }
        sb.append(":" + error);
        print("识别失败：" + sb.toString());
    }

    //识别结果中包含:识别结果、原始结果等,具体见结果解析一节
    @Override
    public void onResults(Bundle results) {
        long end2finish = System.currentTimeMillis() - speechEndTime;
        ArrayList<String> nbest = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        print("识别成功：" + Arrays.toString(nbest.toArray(new String[nbest.size()])));
        String json_res = results.getString("origin_result");
        try {
            print("origin_result=\n" + new JSONObject(json_res).toString(4));
        } catch (Exception e) {
            print("origin_result=[warning: bad json]\n" + json_res);
        }

        mStartVoice.setText("开Start");
        String strEnd2Finish = "";
        if (end2finish < 60 * 1000) {
            strEnd2Finish = "(waited " + end2finish + "ms)";
        }

        mOriginLanguage.setText(nbest.get(0) + strEnd2Finish);
    }

    @Override
    public void onPartialResults(Bundle partialResults) {
        ArrayList<String> nbest = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (nbest.size() > 0) {
            print("~临时识别结果：" + Arrays.toString(nbest.toArray(new String[0])));
            mOriginLanguage.setText(nbest.get(0));
        }
    }

    @Override
    public void onEvent(int eventType, Bundle params) {
        switch (eventType) {
            case EVENT_ERROR:
                String reason = params.get("reason") + "";
                print("EVENT_ERROR, " + reason);
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mSpeechRecognizer != null)
            mSpeechRecognizer.destroy();
    }

    private void print(String msg) {
        mTransferLanuage.append(msg + "\n");
        ScrollView sv = (ScrollView) mTransferLanuage.getParent();
        sv.smoothScrollTo(0, 1000000);
        Log.d(TAG, "----" + msg);
    }
}
