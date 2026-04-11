import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

String API_URL = "http://47.105.51.84/api/relay/call";
String TARGET_GROUP = "883640898";
String TARGET_NAME = "元启Ai交流群";

int BG_PANEL = Color.parseColor("#1A1A2E");
int BG_CARD = Color.parseColor("#252540");
int BG_INPUT = Color.parseColor("#2D2D4A");
int TEXT_MAIN = Color.parseColor("#FFFFFF");
int TEXT_SUB = Color.parseColor("#A0A0B0");
int TEXT_HINT = Color.parseColor("#606080");
int ACCENT_BLUE = Color.parseColor("#4A9EFF");
int ACCENT_GREEN = Color.parseColor("#4AFF9E");
int ACCENT_RED = Color.parseColor("#FF6B6B");
int ACCENT_GOLD = Color.parseColor("#FFD93D");

Map paiLastReplyTime = new HashMap();

String dp(Context ctx, float v) {
    return String.valueOf((int)(v * ctx.getResources().getDisplayMetrics().density + 0.5f));
}

int dpInt(Context ctx, float v) {
    return (int)(v * ctx.getResources().getDisplayMetrics().density + 0.5f);
}

boolean isEmpty(String s) {
    return s == null || s.trim().length() == 0;
}

String safeStr(Object o) {
    return o == null ? "" : String.valueOf(o);
}

String digits(String s) {
    return s == null ? "" : s.replaceAll("[^0-9]", "");
}

String getApiKey() {
    Object v = getData("api_key");
    return v == null ? "" : String.valueOf(v);
}

String getErrorReply() {
    Object v = getData("error_reply");
    return v == null ? "" : String.valueOf(v);
}

int getContextRounds() {
    Object v = getData("context_rounds");
    if (v == null) return 0;
    try {
        int r = Integer.parseInt(String.valueOf(v));
        return r < 0 ? 0 : (r > 20 ? 20 : r);
    } catch (Throwable e) {
        return 0;
    }
}

JSONArray getRules() {
    try {
        Object v = getData("rules");
        if (v == null) return new JSONArray();
        return new JSONArray(String.valueOf(v));
    } catch (Throwable e) {
        return new JSONArray();
    }
}

void saveRules(JSONArray arr) {
    setData("rules", arr == null ? "[]" : arr.toString());
}

void addRule(String mode, String trigger) {
    JSONArray rules = getRules();
    JSONObject r = new JSONObject();
    try {
        r.put("mode", mode);
        r.put("trigger", trigger);
        r.put("enabled", true);
        rules.put(r);
        saveRules(rules);
    } catch (Throwable e) {
        error(e);
    }
}

void deleteRule(int idx) {
    JSONArray rules = getRules();
    JSONArray newArr = new JSONArray();
    for (int i = 0; i < rules.length(); i++) {
        if (i != idx) newArr.put(rules.optJSONObject(i));
    }
    saveRules(newArr);
}

boolean isScopeEnabled(String key) {
    String dataKey = "enabled_" + key;
    Object v = getData(dataKey);
    if (v == null) return false;
    if (v instanceof Boolean) return (Boolean) v;
    try {
        return Boolean.parseBoolean(String.valueOf(v));
    } catch (Throwable e) {
        return false;
    }
}

void setScopeEnabled(String key, boolean en) {
    setData("enabled_" + key, en);
}

boolean isAutoQuote(String key) {
    return getBoolData("auto_quote_" + key, false);
}

boolean isIgnoreReply(String key) {
    return getBoolData("ignore_reply_" + key, true);
}

boolean isReplySelf(String key) {
    return getBoolData("reply_self_" + key, false);
}

boolean isPaiReply(String key) {
    return getBoolData("pai_reply_" + key, false);
}

boolean useKnowledgeBase() {
    return getBoolData("use_knowledge_base", false);
}

int getPaiCooldown() {
    Object v = getData("pai_cooldown");
    if (v == null) return 10;
    try {
        int cd = Integer.parseInt(String.valueOf(v));
        return cd < 0 ? 0 : cd;
    } catch (Throwable e) {
        return 10;
    }
}

boolean canPaiReply(String scopeKey) {
    Long lastTime = (Long) paiLastReplyTime.get(scopeKey);
    if (lastTime == null) return true;
    int cooldown = getPaiCooldown();
    if (cooldown <= 0) return true;
    return (System.currentTimeMillis() - lastTime) > (cooldown * 1000);
}

void updatePaiReplyTime(String scopeKey) {
    paiLastReplyTime.put(scopeKey, System.currentTimeMillis());
}

String getMsgText(Object msg) {
    try {
        StringBuilder sb = new StringBuilder();
        
        String content = msg.Content;
        
        Object m = msg.msg;
        if (m != null) {
            try {
                List elements = m.elements;
                if (elements != null && !elements.isEmpty()) {
                    for (int i = 0; i < elements.size(); i++) {
                        Object elem = elements.get(i);
                        if (elem == null) continue;
                        try {
                            Object textElem = elem.textElement;
                            if (textElem != null) {
                                String text = textElem.content;
                                if (text != null && text.length() > 0) {
                                    sb.append(text);
                                }
                            }
                        } catch (Throwable e) {}
                    }
                }
            } catch (Throwable e) {
                info("[getMsgText] 遍历elements失败: " + e.getMessage());
            }
        }
        
        String result = sb.toString().trim();
        if (result.length() == 0 && content != null) {
            result = content.trim();
        }
        return result;
    } catch (Throwable e) {
        info("[getMsgText] 异常: " + e.getMessage());
        return "";
    }
}

Object getField(Object obj, String name) {
    if (obj == null) return null;
    Class clazz = obj.getClass();
    while (clazz != null) {
        try {
            java.lang.reflect.Field f = clazz.getDeclaredField(name);
            f.setAccessible(true);
            return f.get(obj);
        } catch (Throwable ignore) {}
        clazz = clazz.getSuperclass();
    }
    return null;
}

String getImgUrl(Object msg) {
    try {
        Object picUrlList = getField(msg, "PicUrlList");
        if (picUrlList instanceof ArrayList) {
            ArrayList list = (ArrayList) picUrlList;
            if (list.size() > 0) {
                return list.get(0).toString();
            }
        }
    } catch (Throwable e) {}
    return null;
}

boolean isAtMe(Object msg) {
    try {
        Map atList = msg.AtList;
        if (atList == null || atList.isEmpty()) return false;
        if (atList.containsKey(Long.parseLong(myUin))) return true;
        if (atList.containsKey(myUin)) return true;
    } catch (Throwable e) {
        info("[isAtMe] 异常: " + e.getMessage());
    }
    return false;
}

String[] parsePaiPai(Object msg) {
    try {
        Object rawMsg = msg.msg;
        if (rawMsg == null) return null;
        String rawStr = rawMsg.toString();
        
        if (!rawStr.contains("GrayTipElement") && !rawStr.contains("templParam")) {
            return null;
        }
        
        String senderUin = null;
        String targetUin = null;
        
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("uin_str1=(\\d+)");
        java.util.regex.Matcher matcher = pattern.matcher(rawStr);
        if (matcher.find()) {
            senderUin = matcher.group(1);
        }
        
        pattern = java.util.regex.Pattern.compile("uin_str2=(\\d+)");
        matcher = pattern.matcher(rawStr);
        if (matcher.find()) {
            targetUin = matcher.group(1);
        }
        
        if (senderUin != null && targetUin != null) {
            return new String[]{senderUin, targetUin};
        }
    } catch (Throwable e) {}
    return null;
}

boolean isGroupMsg(Object msg) {
    try {
        return msg.chatType == 2;
    } catch (Throwable e) {
        return false;
    }
}

String getGroupUin(Object msg) {
    try {
        return digits(msg.peerUid);
    } catch (Throwable e) {
        return "";
    }
}

String getSenderUin(Object msg) {
    try {
        return digits(msg.senderUin);
    } catch (Throwable e) {
        return "";
    }
}

Object getContact(Object msg) {
    try {
        return msg.Contact;
    } catch (Throwable e) {
        return null;
    }
}

long getMsgId(Object msg) {
    try {
        Object m = msg.msg;
        if (m != null) {
            return Long.parseLong(String.valueOf(m.msgId));
        }
    } catch (Throwable e) {}
    return 0;
}

int getMsgType(Object msg) {
    try {
        return msg.msgType;
    } catch (Throwable e) {
        return 0;
    }
}

boolean isSendByMe(Object msg) {
    try {
        return msg.MySent;
    } catch (Throwable e) {
        return false;
    }
}

String buildScopeKey(String groupUin, String uin, int chatType) {
    if (chatType == 2) {
        String g = digits(groupUin);
        return isEmpty(g) ? "" : "group_" + g;
    }
    if (chatType == 1 || chatType == 100) {
        String p = digits(uin);
        return isEmpty(p) ? "" : "private_" + p;
    }
    return "";
}

String buildScopeKeyForMsg(Object msg) {
    if (isGroupMsg(msg)) {
        String g = getGroupUin(msg);
        return isEmpty(g) ? "" : "group_" + g;
    }
    String p = getSenderUin(msg);
    return isEmpty(p) ? "" : "private_" + p;
}

String buildScopeLabel(String groupUin, String uin, int chatType) {
    if (chatType == 2) return "当前群聊 " + digits(groupUin);
    String p = digits(uin);
    return isEmpty(p) ? "当前会话" : "当前私聊 " + p;
}

String removeAtTags(String text) {
    if (isEmpty(text)) return "";
    String r = text;
    r = r.replaceAll("\\[PicUrl=[^\\]]*\\]", "");
    r = r.replaceAll("\\[atUin=\\d+[^\\]]*\\]", "");
    r = r.replaceAll("\\[AtQQ=\\d+[^\\]]*\\]", "");
    r = r.replaceAll("\\[At[^\\]]*\\]", "");
    r = r.replaceAll("@[^\\s]+", "");
    r = r.replaceAll("[\\u2066\\u2067\\u2068\\u2069]", "");
    r = r.replaceAll("\\s+", " ").trim();
    return r;
}

String callAI(String msg, String imgUrl, String sender) {
    String apiKey = getApiKey();
    if (isEmpty(API_URL) || isEmpty(apiKey)) {
        return "";
    }

    HttpURLConnection conn = null;
    String result = "";
    try {
        URL url = new URL(API_URL);
        conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("X-API-Key", apiKey);
        conn.setDoOutput(true);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(60000);

        JSONObject body = new JSONObject();
        body.put("message", msg);
        if (!isEmpty(imgUrl)) body.put("imageUrl", imgUrl);
        if (!isEmpty(sender)) body.put("sender", sender);
        body.put("contextRounds", getContextRounds());
        body.put("useKnowledgeBase", useKnowledgeBase());

        OutputStream os = conn.getOutputStream();
        os.write(body.toString().getBytes("UTF-8"));
        os.close();

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            return "";
        }

        InputStream is = conn.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        reader.close();

        String resp = sb.toString().trim();
        if (isEmpty(resp)) {
            return "";
        }

        try {
            JSONObject json = new JSONObject(resp);
            int code = json.optInt("code", -1);
            if (code != 200) {
                return "";
            }
            Object data = json.opt("data");
            if (data == null) {
                return "";
            }
            result = data instanceof String ? (String) data : data.toString();
        } catch (Throwable e) {
            result = resp;
        }
    } catch (Throwable e) {
        info("[callAI] 异常: " + e.getMessage());
        error(e);
    } finally {
        if (conn != null) try { conn.disconnect(); } catch (Throwable e) {}
    }
    
    return result;
}

GradientDrawable panelBg(Context ctx) {
    GradientDrawable gd = new GradientDrawable();
    gd.setColor(BG_PANEL);
    gd.setCornerRadius(dpInt(ctx, 20));
    return gd;
}

GradientDrawable cardBg(Context ctx) {
    GradientDrawable gd = new GradientDrawable();
    gd.setColor(BG_CARD);
    gd.setCornerRadius(dpInt(ctx, 12));
    return gd;
}

GradientDrawable inputBg(Context ctx) {
    GradientDrawable gd = new GradientDrawable();
    gd.setColor(BG_INPUT);
    gd.setCornerRadius(dpInt(ctx, 8));
    return gd;
}

GradientDrawable chipBg(Context ctx, boolean active) {
    GradientDrawable gd = new GradientDrawable();
    if (active) {
        gd.setColor(Color.parseColor("#1A3A2A"));
        gd.setStroke(dpInt(ctx, 1), ACCENT_GREEN);
    } else {
        gd.setColor(BG_INPUT);
    }
    gd.setCornerRadius(dpInt(ctx, 99));
    return gd;
}

GradientDrawable modeChipBg(Context ctx, String mode) {
    GradientDrawable gd = new GradientDrawable();
    gd.setColor(BG_INPUT);
    gd.setCornerRadius(dpInt(ctx, 6));
    int c = ACCENT_BLUE;
    if ("regex".equals(mode)) c = ACCENT_GOLD;
    else if ("at".equals(mode)) c = ACCENT_GREEN;
    gd.setStroke(dpInt(ctx, 1), c);
    return gd;
}

int modeColor(String mode) {
    if ("regex".equals(mode)) return ACCENT_GOLD;
    if ("at".equals(mode)) return ACCENT_GREEN;
    return ACCENT_BLUE;
}

String modeLabel(String mode) {
    if ("regex".equals(mode)) return "正则";
    if ("at".equals(mode)) return "艾特";
    return "关键词";
}

View sectionTitle(Context ctx, String text) {
    TextView tv = new TextView(ctx);
    tv.setText(text);
    tv.setTextSize(12);
    tv.setTextColor(TEXT_SUB);
    tv.setPadding(0, dpInt(ctx, 16), 0, dpInt(ctx, 8));
    return tv;
}

boolean getBoolData(String key, boolean defVal) {
    Object v = getData(key);
    if (v == null) return defVal;
    if (v instanceof Boolean) return (Boolean) v;
    try {
        return Boolean.parseBoolean(String.valueOf(v));
    } catch (Throwable e) {
        return defVal;
    }
}

void setBoolData(String key, boolean val) {
    setData(key, val);
}

View toggleItem(Context ctx, String label, boolean checked, String key) {
    LinearLayout item = new LinearLayout(ctx);
    item.setOrientation(LinearLayout.HORIZONTAL);
    item.setGravity(Gravity.CENTER_VERTICAL);
    item.setBackground(cardBg(ctx));
    item.setPadding(dpInt(ctx, 16), dpInt(ctx, 14), dpInt(ctx, 16), dpInt(ctx, 14));
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    params.bottomMargin = dpInt(ctx, 8);
    item.setLayoutParams(params);

    TextView labelTv = new TextView(ctx);
    labelTv.setText(label);
    labelTv.setTextSize(14);
    labelTv.setTextColor(TEXT_MAIN);
    labelTv.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
    item.addView(labelTv);

    TextView toggle = new TextView(ctx);
    toggle.setText(checked ? "开" : "关");
    toggle.setTextSize(12);
    toggle.setTextColor(checked ? TEXT_MAIN : TEXT_SUB);
    toggle.setBackground(chipBg(ctx, checked));
    toggle.setPadding(dpInt(ctx, 12), dpInt(ctx, 4), dpInt(ctx, 12), dpInt(ctx, 4));
    item.addView(toggle);

    item.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            boolean currentVal = getBoolData(key, false);
            boolean newVal = !currentVal;
            setBoolData(key, newVal);
            toggle.setText(newVal ? "开" : "关");
            toggle.setTextColor(newVal ? TEXT_MAIN : TEXT_SUB);
            toggle.setBackground(chipBg(ctx, newVal));
        }
    });

    return item;
}

View toggleItemWithHint(Context ctx, String label, boolean checked, String key, String hint) {
    LinearLayout item = new LinearLayout(ctx);
    item.setOrientation(LinearLayout.VERTICAL);
    item.setBackground(cardBg(ctx));
    item.setPadding(dpInt(ctx, 16), dpInt(ctx, 14), dpInt(ctx, 16), dpInt(ctx, 14));
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    params.bottomMargin = dpInt(ctx, 8);
    item.setLayoutParams(params);

    LinearLayout row = new LinearLayout(ctx);
    row.setOrientation(LinearLayout.HORIZONTAL);
    row.setGravity(Gravity.CENTER_VERTICAL);

    TextView labelTv = new TextView(ctx);
    labelTv.setText(label);
    labelTv.setTextSize(14);
    labelTv.setTextColor(TEXT_MAIN);
    labelTv.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
    row.addView(labelTv);

    TextView toggle = new TextView(ctx);
    toggle.setText(checked ? "开" : "关");
    toggle.setTextSize(12);
    toggle.setTextColor(checked ? TEXT_MAIN : TEXT_SUB);
    toggle.setBackground(chipBg(ctx, checked));
    toggle.setPadding(dpInt(ctx, 12), dpInt(ctx, 4), dpInt(ctx, 12), dpInt(ctx, 4));
    row.addView(toggle);
    item.addView(row);

    TextView hintTv = new TextView(ctx);
    hintTv.setText(hint);
    hintTv.setTextSize(10);
    hintTv.setTextColor(TEXT_HINT);
    hintTv.setPadding(0, dpInt(ctx, 4), 0, 0);
    item.addView(hintTv);

    item.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            boolean currentVal = getBoolData(key, false);
            boolean newVal = !currentVal;
            setBoolData(key, newVal);
            toggle.setText(newVal ? "开" : "关");
            toggle.setTextColor(newVal ? TEXT_MAIN : TEXT_SUB);
            toggle.setBackground(chipBg(ctx, newVal));
        }
    });

    return item;
}

View rulesCard(Context ctx, JSONArray rules, String scopeKey, boolean isGroup, AlertDialog dialog, Activity activity) {
    LinearLayout card = new LinearLayout(ctx);
    card.setOrientation(LinearLayout.VERTICAL);
    card.setBackground(cardBg(ctx));
    card.setPadding(dpInt(ctx, 16), dpInt(ctx, 14), dpInt(ctx, 16), dpInt(ctx, 14));
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    params.bottomMargin = dpInt(ctx, 8);
    card.setLayoutParams(params);

    LinearLayout header = new LinearLayout(ctx);
    header.setOrientation(LinearLayout.HORIZONTAL);
    header.setGravity(Gravity.CENTER_VERTICAL);

    TextView title = new TextView(ctx);
    title.setText("已配置 " + rules.length() + " 条规则");
    title.setTextSize(12);
    title.setTextColor(TEXT_SUB);
    title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
    header.addView(title);

    TextView addBtn = new TextView(ctx);
    addBtn.setText("+ 添加");
    addBtn.setTextSize(12);
    addBtn.setTextColor(ACCENT_BLUE);
    addBtn.setPadding(dpInt(ctx, 8), dpInt(ctx, 4), dpInt(ctx, 8), dpInt(ctx, 4));
    header.addView(addBtn);
    card.addView(header);

    if (rules.length() > 0) {
        for (int i = 0; i < rules.length(); i++) {
            JSONObject rule = rules.optJSONObject(i);
            if (rule == null) continue;

            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dpInt(ctx, 8), 0, 0);

            String mode = rule.optString("mode", "keyword");
            String trigger = rule.optString("trigger", "");

            TextView modeChip = new TextView(ctx);
            modeChip.setText(modeLabel(mode));
            modeChip.setTextSize(10);
            modeChip.setTextColor(modeColor(mode));
            modeChip.setBackground(modeChipBg(ctx, mode));
            modeChip.setPadding(dpInt(ctx, 6), dpInt(ctx, 2), dpInt(ctx, 6), dpInt(ctx, 2));
            row.addView(modeChip);

            TextView triggerTv = new TextView(ctx);
            triggerTv.setText(" " + trigger);
            triggerTv.setTextSize(12);
            triggerTv.setTextColor(TEXT_MAIN);
            triggerTv.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            row.addView(triggerTv);

            int idx = i;
            String sk = scopeKey;
            boolean ig = isGroup;
            TextView delBtn = new TextView(ctx);
            delBtn.setText("删除");
            delBtn.setTextSize(10);
            delBtn.setTextColor(ACCENT_RED);
            delBtn.setPadding(dpInt(ctx, 8), dpInt(ctx, 2), dpInt(ctx, 8), dpInt(ctx, 2));
            delBtn.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    deleteRule(idx);
                    Toast("已删除");
                    if (dialog != null) {
                        dialog.dismiss();
                    }
                    if (activity != null) {
                        showMainMenu(sk.replace("group_", "").replace("private_", ""), "", ig ? 2 : 1);
                    }
                }
            });
            row.addView(delBtn);
            card.addView(row);
        }
    }

    String sk = scopeKey;
    boolean ig = isGroup;
    AlertDialog md = dialog;
    addBtn.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            showAddRuleDialog((Activity) ctx, sk, ig, md);
        }
    });

    return card;
}

View scopeManager(Context ctx, String scopeKey, boolean isGroup) {
    LinearLayout card = new LinearLayout(ctx);
    card.setOrientation(LinearLayout.VERTICAL);
    card.setBackground(cardBg(ctx));
    card.setPadding(dpInt(ctx, 16), dpInt(ctx, 14), dpInt(ctx, 16), dpInt(ctx, 14));
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    params.bottomMargin = dpInt(ctx, 8);
    card.setLayoutParams(params);

    TextView label = new TextView(ctx);
    label.setText(isGroup ? "当前群聊" : "当前私聊");
    label.setTextSize(12);
    label.setTextColor(TEXT_SUB);
    card.addView(label);

    LinearLayout row = new LinearLayout(ctx);
    row.setOrientation(LinearLayout.HORIZONTAL);
    row.setGravity(Gravity.CENTER_VERTICAL);
    row.setPadding(0, dpInt(ctx, 8), 0, 0);

    String displayText = isGroup 
        ? "群号: " + scopeKey.replace("group_", "") 
        : "QQ号: " + scopeKey.replace("private_", "");
    TextView info = new TextView(ctx);
    info.setText(displayText);
    info.setTextSize(14);
    info.setTextColor(TEXT_MAIN);
    info.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
    row.addView(info);

    TextView toggle = new TextView(ctx);
    boolean enabled = isScopeEnabled(scopeKey);
    toggle.setText(enabled ? "已开启" : "已关闭");
    toggle.setTextSize(12);
    toggle.setTextColor(enabled ? TEXT_MAIN : TEXT_SUB);
    toggle.setBackground(chipBg(ctx, enabled));
    toggle.setPadding(dpInt(ctx, 12), dpInt(ctx, 4), dpInt(ctx, 12), dpInt(ctx, 4));
    row.addView(toggle);
    card.addView(row);

    String[] keyHolder = {scopeKey};
    TextView[] toggleHolder = {toggle};
    
    row.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            String key = keyHolder[0];
            TextView tv = toggleHolder[0];
            boolean en = isScopeEnabled(key);
            boolean newVal = !en;
            setScopeEnabled(key, newVal);
            tv.setText(newVal ? "已开启" : "已关闭");
            tv.setTextColor(newVal ? TEXT_MAIN : TEXT_SUB);
            tv.setBackground(chipBg(ctx, newVal));
            Toast(newVal ? "已开启" : "已关闭");
        }
    });

    return card;
}

void showApiKeyDialog(Activity activity) {
    AlertDialog dialog = new AlertDialog.Builder(activity).create();
    dialog.setCanceledOnTouchOutside(true);

    LinearLayout root = new LinearLayout(activity);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setBackgroundColor(Color.TRANSPARENT);
    root.setPadding(dpInt(activity, 20), dpInt(activity, 50), dpInt(activity, 20), dpInt(activity, 50));

    LinearLayout panel = new LinearLayout(activity);
    panel.setOrientation(LinearLayout.VERTICAL);
    panel.setBackground(panelBg(activity));
    panel.setPadding(dpInt(activity, 20), dpInt(activity, 24), dpInt(activity, 20), dpInt(activity, 24));

    LinearLayout headerRow = new LinearLayout(activity);
    headerRow.setOrientation(LinearLayout.HORIZONTAL);
    headerRow.setGravity(Gravity.CENTER_VERTICAL);

    TextView title = new TextView(activity);
    title.setText("设置");
    title.setTextSize(20);
    title.setTypeface(null, Typeface.BOLD);
    title.setTextColor(TEXT_MAIN);
    title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
    headerRow.addView(title);

    TextView closeBtn = new TextView(activity);
    closeBtn.setText("✕");
    closeBtn.setTextSize(18);
    closeBtn.setTextColor(TEXT_SUB);
    closeBtn.setPadding(dpInt(activity, 8), dpInt(activity, 4), dpInt(activity, 8), dpInt(activity, 4));
    closeBtn.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            dialog.dismiss();
        }
    });
    headerRow.addView(closeBtn);
    panel.addView(headerRow);

    TextView subLabel = new TextView(activity);
    subLabel.setText("配置元启API密钥与默认回复词");
    subLabel.setTextSize(11);
    subLabel.setTextColor(TEXT_SUB);
    subLabel.setPadding(0, dpInt(activity, 8), 0, dpInt(activity, 16));
    panel.addView(subLabel);

    ScrollView scroll = new ScrollView(activity);
    scroll.setVerticalScrollBarEnabled(false);
    LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpInt(activity, 400));
    scroll.setLayoutParams(scrollParams);

    LinearLayout content = new LinearLayout(activity);
    content.setOrientation(LinearLayout.VERTICAL);

    LinearLayout apiKeyCard = new LinearLayout(activity);
    apiKeyCard.setOrientation(LinearLayout.VERTICAL);
    apiKeyCard.setBackground(cardBg(activity));
    apiKeyCard.setPadding(dpInt(activity, 16), dpInt(activity, 14), dpInt(activity, 16), dpInt(activity, 14));
    LinearLayout.LayoutParams cardParams1 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    cardParams1.bottomMargin = dpInt(activity, 12);
    apiKeyCard.setLayoutParams(cardParams1);

    TextView apiKeyLabel = new TextView(activity);
    apiKeyLabel.setText("元启API Key");
    apiKeyLabel.setTextSize(12);
    apiKeyLabel.setTextColor(TEXT_SUB);
    apiKeyCard.addView(apiKeyLabel);

    EditText apiKeyInput = new EditText(activity);
    apiKeyInput.setText(getApiKey());
    apiKeyInput.setTextColor(TEXT_MAIN);
    apiKeyInput.setHint("请输入元启API Key");
    apiKeyInput.setHintTextColor(TEXT_HINT);
    apiKeyInput.setBackground(inputBg(activity));
    apiKeyInput.setPadding(dpInt(activity, 16), dpInt(activity, 12), dpInt(activity, 16), dpInt(activity, 12));
    apiKeyInput.setFocusable(true);
    apiKeyInput.setFocusableInTouchMode(true);
    LinearLayout.LayoutParams apiKeyParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    apiKeyParams.topMargin = dpInt(activity, 8);
    apiKeyInput.setLayoutParams(apiKeyParams);
    apiKeyCard.addView(apiKeyInput);
    content.addView(apiKeyCard);

    LinearLayout contextCard = new LinearLayout(activity);
    contextCard.setOrientation(LinearLayout.VERTICAL);
    contextCard.setBackground(cardBg(activity));
    contextCard.setPadding(dpInt(activity, 16), dpInt(activity, 14), dpInt(activity, 16), dpInt(activity, 14));
    LinearLayout.LayoutParams cardParams2 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    cardParams2.bottomMargin = dpInt(activity, 12);
    contextCard.setLayoutParams(cardParams2);

    TextView contextLabel = new TextView(activity);
    contextLabel.setText("上下文轮数");
    contextLabel.setTextSize(12);
    contextLabel.setTextColor(TEXT_SUB);
    contextCard.addView(contextLabel);

    TextView contextHint = new TextView(activity);
    contextHint.setText("AI对话记忆轮数，范围0-20，0表示无记忆");
    contextHint.setTextSize(10);
    contextHint.setTextColor(TEXT_HINT);
    contextHint.setPadding(0, dpInt(activity, 4), 0, dpInt(activity, 8));
    contextCard.addView(contextHint);

    EditText contextInput = new EditText(activity);
    contextInput.setText(String.valueOf(getContextRounds()));
    contextInput.setTextColor(TEXT_MAIN);
    contextInput.setHint("0-20");
    contextInput.setHintTextColor(TEXT_HINT);
    contextInput.setInputType(InputType.TYPE_CLASS_NUMBER);
    contextInput.setBackground(inputBg(activity));
    contextInput.setPadding(dpInt(activity, 16), dpInt(activity, 12), dpInt(activity, 16), dpInt(activity, 12));
    contextInput.setFocusable(true);
    contextInput.setFocusableInTouchMode(true);
    contextInput.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    contextCard.addView(contextInput);
    content.addView(contextCard);

    LinearLayout knowledgeCard = new LinearLayout(activity);
    knowledgeCard.setOrientation(LinearLayout.VERTICAL);
    knowledgeCard.setBackground(cardBg(activity));
    knowledgeCard.setPadding(dpInt(activity, 16), dpInt(activity, 14), dpInt(activity, 16), dpInt(activity, 14));
    LinearLayout.LayoutParams cardParams2b = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    cardParams2b.bottomMargin = dpInt(activity, 12);
    knowledgeCard.setLayoutParams(cardParams2b);

    LinearLayout knowledgeRow = new LinearLayout(activity);
    knowledgeRow.setOrientation(LinearLayout.HORIZONTAL);
    knowledgeRow.setGravity(Gravity.CENTER_VERTICAL);

    TextView knowledgeLabel = new TextView(activity);
    knowledgeLabel.setText("启用知识库");
    knowledgeLabel.setTextSize(12);
    knowledgeLabel.setTextColor(TEXT_SUB);
    knowledgeLabel.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
    knowledgeRow.addView(knowledgeLabel);

    final TextView knowledgeToggle = new TextView(activity);
    knowledgeToggle.setText(useKnowledgeBase() ? "开" : "关");
    knowledgeToggle.setTextSize(12);
    knowledgeToggle.setTextColor(useKnowledgeBase() ? TEXT_MAIN : TEXT_SUB);
    knowledgeToggle.setBackground(chipBg(activity, useKnowledgeBase()));
    knowledgeToggle.setPadding(dpInt(activity, 12), dpInt(activity, 4), dpInt(activity, 12), dpInt(activity, 4));
    knowledgeRow.addView(knowledgeToggle);
    knowledgeCard.addView(knowledgeRow);

    TextView knowledgeHint = new TextView(activity);
    knowledgeHint.setText("启用知识库会导致回复变慢");
    knowledgeHint.setTextSize(10);
    knowledgeHint.setTextColor(TEXT_HINT);
    knowledgeHint.setPadding(0, dpInt(activity, 4), 0, 0);
    knowledgeCard.addView(knowledgeHint);

    knowledgeCard.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            boolean newVal = !useKnowledgeBase();
            setBoolData("use_knowledge_base", newVal);
            knowledgeToggle.setText(newVal ? "开" : "关");
            knowledgeToggle.setTextColor(newVal ? TEXT_MAIN : TEXT_SUB);
            knowledgeToggle.setBackground(chipBg(activity, newVal));
        }
    });
    content.addView(knowledgeCard);

    LinearLayout cooldownCard = new LinearLayout(activity);
    cooldownCard.setOrientation(LinearLayout.VERTICAL);
    cooldownCard.setBackground(cardBg(activity));
    cooldownCard.setPadding(dpInt(activity, 16), dpInt(activity, 14), dpInt(activity, 16), dpInt(activity, 14));
    LinearLayout.LayoutParams cardParams2c = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    cardParams2c.bottomMargin = dpInt(activity, 12);
    cooldownCard.setLayoutParams(cardParams2c);

    TextView cooldownLabel = new TextView(activity);
    cooldownLabel.setText("拍一拍限流间隔");
    cooldownLabel.setTextSize(12);
    cooldownLabel.setTextColor(TEXT_SUB);
    cooldownCard.addView(cooldownLabel);

    TextView cooldownHint = new TextView(activity);
    cooldownHint.setText("同一作用域内两次拍一拍回复的最小间隔秒数，0表示不限流");
    cooldownHint.setTextSize(10);
    cooldownHint.setTextColor(TEXT_HINT);
    cooldownHint.setPadding(0, dpInt(activity, 4), 0, dpInt(activity, 8));
    cooldownCard.addView(cooldownHint);

    EditText cooldownInput = new EditText(activity);
    cooldownInput.setText(String.valueOf(getPaiCooldown()));
    cooldownInput.setTextColor(TEXT_MAIN);
    cooldownInput.setHint("默认10秒");
    cooldownInput.setHintTextColor(TEXT_HINT);
    cooldownInput.setInputType(InputType.TYPE_CLASS_NUMBER);
    cooldownInput.setBackground(inputBg(activity));
    cooldownInput.setPadding(dpInt(activity, 16), dpInt(activity, 12), dpInt(activity, 16), dpInt(activity, 12));
    cooldownInput.setFocusable(true);
    cooldownInput.setFocusableInTouchMode(true);
    cooldownInput.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    cooldownCard.addView(cooldownInput);
    content.addView(cooldownCard);

    LinearLayout errorCard = new LinearLayout(activity);
    errorCard.setOrientation(LinearLayout.VERTICAL);
    errorCard.setBackground(cardBg(activity));
    errorCard.setPadding(dpInt(activity, 16), dpInt(activity, 14), dpInt(activity, 16), dpInt(activity, 14));
    LinearLayout.LayoutParams cardParams3 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    cardParams3.bottomMargin = dpInt(activity, 20);
    errorCard.setLayoutParams(cardParams3);

    TextView errorReplyLabel = new TextView(activity);
    errorReplyLabel.setText("默认回复词");
    errorReplyLabel.setTextSize(12);
    errorReplyLabel.setTextColor(TEXT_SUB);
    errorCard.addView(errorReplyLabel);

    TextView errorHint = new TextView(activity);
    errorHint.setText("AI调用失败时发送此内容，留空则不回复");
    errorHint.setTextSize(10);
    errorHint.setTextColor(TEXT_HINT);
    errorHint.setPadding(0, dpInt(activity, 4), 0, dpInt(activity, 8));
    errorCard.addView(errorHint);

    EditText errorReplyInput = new EditText(activity);
    errorReplyInput.setText(getErrorReply());
    errorReplyInput.setTextColor(TEXT_MAIN);
    errorReplyInput.setHint("例如：AI服务暂时不可用");
    errorReplyInput.setHintTextColor(TEXT_HINT);
    errorReplyInput.setBackground(inputBg(activity));
    errorReplyInput.setPadding(dpInt(activity, 16), dpInt(activity, 12), dpInt(activity, 16), dpInt(activity, 12));
    errorReplyInput.setFocusable(true);
    errorReplyInput.setFocusableInTouchMode(true);
    errorReplyInput.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    errorCard.addView(errorReplyInput);
    content.addView(errorCard);
    
    scroll.addView(content);
    panel.addView(scroll);

    LinearLayout btnRow = new LinearLayout(activity);
    btnRow.setOrientation(LinearLayout.HORIZONTAL);
    btnRow.setGravity(Gravity.END);

    TextView cancelBtn = new TextView(activity);
    cancelBtn.setText("取消");
    cancelBtn.setTextSize(13);
    cancelBtn.setTextColor(TEXT_SUB);
    cancelBtn.setBackground(chipBg(activity, false));
    cancelBtn.setPadding(dpInt(activity, 16), dpInt(activity, 10), dpInt(activity, 16), dpInt(activity, 10));
    cancelBtn.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            dialog.dismiss();
        }
    });
    btnRow.addView(cancelBtn);

    TextView saveBtn = new TextView(activity);
    saveBtn.setText("保存设置");
    saveBtn.setTextSize(13);
    saveBtn.setTextColor(TEXT_MAIN);
    saveBtn.setBackground(chipBg(activity, true));
    saveBtn.setPadding(dpInt(activity, 16), dpInt(activity, 10), dpInt(activity, 16), dpInt(activity, 10));
    LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    saveParams.leftMargin = dpInt(activity, 12);
    saveBtn.setLayoutParams(saveParams);
    saveBtn.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            String apiKey = apiKeyInput.getText().toString().trim();
            String errorReply = errorReplyInput.getText().toString().trim();
            String contextStr = contextInput.getText().toString().trim();
            int contextRounds = 0;
            try {
                contextRounds = Integer.parseInt(contextStr);
            } catch (Throwable e) {}
            if (contextRounds < 0) contextRounds = 0;
            if (contextRounds > 20) contextRounds = 20;
            
            String cooldownStr = cooldownInput.getText().toString().trim();
            int cooldown = 10;
            try {
                cooldown = Integer.parseInt(cooldownStr);
            } catch (Throwable e) {}
            if (cooldown < 0) cooldown = 0;
            
            setData("api_key", apiKey);
            setData("error_reply", errorReply);
            setData("context_rounds", contextRounds);
            setData("pai_cooldown", cooldown);
            Toast("设置已保存");
            dialog.dismiss();
        }
    });
    btnRow.addView(saveBtn);
    panel.addView(btnRow);

    root.addView(panel);
    dialog.show();

    Window window = dialog.getWindow();
    if (window != null) {
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.setLayout(dpInt(activity, 320), ViewGroup.LayoutParams.WRAP_CONTENT);
        window.setContentView(root);
        window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE | android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }
}

void showAddRuleDialog(Activity activity, String scopeKey, boolean isGroup, AlertDialog mainDialog) {
    AlertDialog dialog = new AlertDialog.Builder(activity).create();
    dialog.setTitle("添加触发规则");

    LinearLayout layout = new LinearLayout(activity);
    layout.setOrientation(LinearLayout.VERTICAL);
    layout.setPadding(dpInt(activity, 20), dpInt(activity, 16), dpInt(activity, 20), dpInt(activity, 16));

    TextView modeLabelTv = new TextView(activity);
    modeLabelTv.setText("触发方式");
    modeLabelTv.setTextSize(12);
    modeLabelTv.setTextColor(TEXT_SUB);
    layout.addView(modeLabelTv);

    LinearLayout modeRow = new LinearLayout(activity);
    modeRow.setOrientation(LinearLayout.HORIZONTAL);
    modeRow.setPadding(0, dpInt(activity, 8), 0, dpInt(activity, 8));

    int[] selectedMode = {0};

    TextView btnKeyword = new TextView(activity);
    btnKeyword.setText("关键词");
    btnKeyword.setTextSize(11);
    btnKeyword.setTextColor(TEXT_MAIN);
    btnKeyword.setBackground(modeChipBg(activity, "keyword"));
    btnKeyword.setPadding(dpInt(activity, 10), dpInt(activity, 6), dpInt(activity, 10), dpInt(activity, 6));
    LinearLayout.LayoutParams btnParams1 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    btnParams1.rightMargin = dpInt(activity, 8);
    btnKeyword.setLayoutParams(btnParams1);
    modeRow.addView(btnKeyword);

    TextView btnRegex = new TextView(activity);
    btnRegex.setText("正则");
    btnRegex.setTextSize(11);
    btnRegex.setTextColor(TEXT_SUB);
    btnRegex.setBackground(modeChipBg(activity, "regex"));
    btnRegex.setPadding(dpInt(activity, 10), dpInt(activity, 6), dpInt(activity, 10), dpInt(activity, 6));
    LinearLayout.LayoutParams btnParams2 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    btnParams2.rightMargin = dpInt(activity, 8);
    btnRegex.setLayoutParams(btnParams2);
    modeRow.addView(btnRegex);

    TextView btnAt = new TextView(activity);
    btnAt.setText("艾特");
    btnAt.setTextSize(11);
    btnAt.setTextColor(TEXT_SUB);
    btnAt.setBackground(modeChipBg(activity, "at"));
    btnAt.setPadding(dpInt(activity, 10), dpInt(activity, 6), dpInt(activity, 10), dpInt(activity, 6));
    LinearLayout.LayoutParams btnParams3 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    btnParams3.rightMargin = dpInt(activity, 8);
    btnAt.setLayoutParams(btnParams3);

    if (isGroup) {
        modeRow.addView(btnAt);
    }

    layout.addView(modeRow);

    EditText triggerInput = new EditText(activity);
    triggerInput.setHint("触发词/正则表达式（艾特触发无需填写）");
    triggerInput.setTextColor(TEXT_MAIN);
    triggerInput.setHintTextColor(TEXT_HINT);
    triggerInput.setBackground(inputBg(activity));
    triggerInput.setPadding(dpInt(activity, 16), dpInt(activity, 12), dpInt(activity, 16), dpInt(activity, 12));
    triggerInput.setFocusable(true);
    triggerInput.setFocusableInTouchMode(true);
    LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    inputParams.topMargin = dpInt(activity, 8);
    triggerInput.setLayoutParams(inputParams);
    layout.addView(triggerInput);

    btnKeyword.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            selectedMode[0] = 0;
            btnKeyword.setTextColor(TEXT_MAIN);
            btnRegex.setTextColor(TEXT_SUB);
            btnAt.setTextColor(TEXT_SUB);
        }
    });

    btnRegex.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            selectedMode[0] = 1;
            btnKeyword.setTextColor(TEXT_SUB);
            btnRegex.setTextColor(TEXT_MAIN);
            btnAt.setTextColor(TEXT_SUB);
        }
    });

    btnAt.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            selectedMode[0] = 2;
            btnKeyword.setTextColor(TEXT_SUB);
            btnRegex.setTextColor(TEXT_SUB);
            btnAt.setTextColor(TEXT_MAIN);
        }
    });

    dialog.setView(layout);

    String sk = scopeKey;
    boolean ig = isGroup;
    dialog.setButton(AlertDialog.BUTTON_POSITIVE, "添加", new android.content.DialogInterface.OnClickListener() {
        public void onClick(android.content.DialogInterface dialog, int which) {
            String trigger = triggerInput.getText().toString().trim();
            if (selectedMode[0] != 2 && isEmpty(trigger)) {
                Toast("请输入触发词");
                return;
            }

            if (selectedMode[0] == 1) {
                try {
                    Pattern.compile(trigger);
                } catch (Throwable e) {
                    Toast("正则表达式无效");
                    return;
                }
            }

            String[] modes = {"keyword", "regex", "at"};
            addRule(modes[selectedMode[0]], selectedMode[0] == 2 ? "@我" : trigger);
            Toast("已添加");
            if (mainDialog != null) {
                mainDialog.dismiss();
            }
            showMainMenu(sk.replace("group_", "").replace("private_", ""), "", ig ? 2 : 1);
        }
    });

    dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "取消", new android.content.DialogInterface.OnClickListener() {
        public void onClick(android.content.DialogInterface dialog, int which) {
            dialog.dismiss();
        }
    });

    dialog.show();

    Window window = dialog.getWindow();
    if (window != null) {
        window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE | android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }
}

void showJoinGroupDialog(Activity activity) {
    if (activity == null) return;
    activity.runOnUiThread(new Runnable() {
        public void run() {
            try {
                AlertDialog dialog = new AlertDialog.Builder(activity).create();
                dialog.setCancelable(false);

                LinearLayout root = new LinearLayout(activity);
                root.setOrientation(LinearLayout.VERTICAL);
                root.setBackgroundColor(Color.TRANSPARENT);
                root.setPadding(dpInt(activity, 20), dpInt(activity, 50), dpInt(activity, 20), dpInt(activity, 50));

                LinearLayout panel = new LinearLayout(activity);
                panel.setOrientation(LinearLayout.VERTICAL);
                panel.setBackground(panelBg(activity));
                panel.setPadding(dpInt(activity, 20), dpInt(activity, 24), dpInt(activity, 20), dpInt(activity, 24));

                TextView title = new TextView(activity);
                title.setText("欢迎使用元启Ai");
                title.setTextSize(20);
                title.setTypeface(null, Typeface.BOLD);
                title.setTextColor(TEXT_MAIN);
                title.setGravity(Gravity.CENTER);
                title.setPadding(0, 0, 0, dpInt(activity, 12));
                panel.addView(title);

                TextView content = new TextView(activity);
                content.setText("为了获取更好的体验和脚本的最新更新，请加入我们的官方交流群！");
                content.setTextSize(13);
                content.setTextColor(TEXT_SUB);
                content.setPadding(0, 0, 0, dpInt(activity, 16));
                content.setLineSpacing(dpInt(activity, 4), 1.2f);
                panel.addView(content);

                LinearLayout groupCard = new LinearLayout(activity);
                groupCard.setOrientation(LinearLayout.VERTICAL);
                groupCard.setBackground(cardBg(activity));
                groupCard.setPadding(dpInt(activity, 16), dpInt(activity, 14), dpInt(activity, 16), dpInt(activity, 14));
                LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                cardParams.bottomMargin = dpInt(activity, 16);
                groupCard.setLayoutParams(cardParams);

                TextView groupName = new TextView(activity);
                groupName.setText(TARGET_NAME);
                groupName.setTextSize(16);
                groupName.setTypeface(null, Typeface.BOLD);
                groupName.setTextColor(TEXT_MAIN);
                groupCard.addView(groupName);

                TextView groupUin = new TextView(activity);
                groupUin.setText("群号: " + TARGET_GROUP);
                groupUin.setTextSize(12);
                groupUin.setTextColor(TEXT_SUB);
                groupUin.setPadding(0, dpInt(activity, 4), 0, 0);
                groupCard.addView(groupUin);

                groupCard.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        try {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("mqqapi://card/show_pslcard?src_type=internal&version=1&uin=" + TARGET_GROUP + "&card_type=group&source=qrcode"));
                            activity.startActivity(intent);
                        } catch (Exception e) {
                            Toast("跳转失败，请检查QQ版本");
                        }
                    }
                });

                panel.addView(groupCard);

                TextView closeBtn = new TextView(activity);
                closeBtn.setText("我已加群 (5s)");
                closeBtn.setTextSize(13);
                closeBtn.setTextColor(TEXT_MAIN);
                closeBtn.setGravity(Gravity.CENTER);
                closeBtn.setBackground(chipBg(activity, false));
                closeBtn.setPadding(dpInt(activity, 16), dpInt(activity, 12), dpInt(activity, 16), dpInt(activity, 12));
                panel.addView(closeBtn);

                root.addView(panel);
                dialog.show();

                Window window = dialog.getWindow();
                if (window != null) {
                    window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                    window.setLayout(dpInt(activity, 300), ViewGroup.LayoutParams.WRAP_CONTENT);
                    window.setContentView(root);
                }

                Handler handler = new Handler(Looper.getMainLooper());
                int[] seconds = {5};
                Runnable countdown = new Runnable() {
                    public void run() {
                        seconds[0]--;
                        if (seconds[0] > 0) {
                            closeBtn.setText("我已加群 (" + seconds[0] + "s)");
                            handler.postDelayed(this, 1000);
                        } else {
                            closeBtn.setText("关闭");
                            closeBtn.setBackground(chipBg(activity, true));
                            closeBtn.setOnClickListener(new View.OnClickListener() {
                                public void onClick(View v) {
                                    dialog.dismiss();
                                }
                            });
                        }
                    }
                };
                handler.postDelayed(countdown, 1000);

            } catch (Exception e) {
                Toast("显示加群弹窗失败");
            }
        }
    });
}

void checkJoinGroup() {
    int[] retryCount = {0};
    int maxRetry = 5;
    Handler groupHandler = new Handler(Looper.getMainLooper());
    Runnable checkGroup = new Runnable() {
        public void run() {
            Activity activity = getThreadActivity();
            if (activity == null) {
                retryCount[0]++;
                if (retryCount[0] < maxRetry) {
                    groupHandler.postDelayed(this, 1000);
                }
                return;
            }

            ArrayList joinedGroups = new ArrayList();
            try {
                List groupList = groups();
                if (groupList != null && !groupList.isEmpty()) {
                    for (int i = 0; i < groupList.size(); i++) {
                        HashMap group = (HashMap) groupList.get(i);
                        joinedGroups.add(group.get("group"));
                    }
                }
            } catch (Throwable e) {
                error("获取群列表失败: " + e.getMessage());
            }

            if (joinedGroups.isEmpty()) {
                retryCount[0]++;
                if (retryCount[0] < maxRetry) {
                    groupHandler.postDelayed(this, 1000);
                }
                return;
            }

            boolean hasJoined = false;
            for (Object groupUin : joinedGroups) {
                String gu = String.valueOf(groupUin);
                if (TARGET_GROUP.equals(gu)) {
                    hasJoined = true;
                    break;
                }
            }

            if (!hasJoined) {
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        showJoinGroupDialog(activity);
                    }
                });
            }
        }
    };
    groupHandler.postDelayed(checkGroup, 1000);
}

public void showMainMenu(String groupUin, String uin, int chatType) {
    Activity activity = getThreadActivity();
    if (activity == null) {
        Toast("无法获取当前界面");
        return;
    }

    String actualUin = uin;
    if (chatType == 1 || chatType == 100) {
        if (!isEmpty(groupUin) && !groupUin.equals(myUin)) {
            actualUin = groupUin;
        }
    }

    String scopeKey = buildScopeKey(groupUin, actualUin, chatType);
    String scopeLabel = buildScopeLabel(groupUin, actualUin, chatType);
    boolean isGroup = chatType == 2;

    String[] scopeKeyHolder = {scopeKey};
    String[] scopeLabelHolder = {scopeLabel};
    boolean[] isGroupHolder = {isGroup};

    activity.runOnUiThread(new Runnable() {
        public void run() {
            try {
                String sk = scopeKeyHolder[0];
                String sl = scopeLabelHolder[0];
                boolean ig = isGroupHolder[0];
                
                AlertDialog dialog = new AlertDialog.Builder(activity).create();
                dialog.setCanceledOnTouchOutside(true);

                LinearLayout root = new LinearLayout(activity);
                root.setOrientation(LinearLayout.VERTICAL);
                root.setBackgroundColor(Color.TRANSPARENT);
                root.setPadding(dpInt(activity, 20), dpInt(activity, 50), dpInt(activity, 20), dpInt(activity, 50));

                LinearLayout panel = new LinearLayout(activity);
                panel.setOrientation(LinearLayout.VERTICAL);
                panel.setBackground(panelBg(activity));
                panel.setPadding(dpInt(activity, 20), dpInt(activity, 24), dpInt(activity, 20), dpInt(activity, 24));

                LinearLayout headerRow = new LinearLayout(activity);
                headerRow.setOrientation(LinearLayout.HORIZONTAL);
                headerRow.setGravity(Gravity.CENTER_VERTICAL);

                TextView title = new TextView(activity);
                title.setText("元启Ai");
                title.setTextSize(20);
                title.setTypeface(null, Typeface.BOLD);
                title.setTextColor(TEXT_MAIN);
                title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                headerRow.addView(title);

                TextView slogan = new TextView(activity);
                slogan.setText("执代码为斧，劈智能之蒙昧");
                slogan.setTextSize(9);
                slogan.setTextColor(TEXT_HINT);
                slogan.setPadding(0, 0, dpInt(activity, 8), 0);
                headerRow.addView(slogan);

                TextView settingsBtn = new TextView(activity);
                settingsBtn.setText("⚙");
                settingsBtn.setTextSize(22);
                settingsBtn.setTextColor(TEXT_SUB);
                settingsBtn.setPadding(dpInt(activity, 8), dpInt(activity, 4), dpInt(activity, 8), dpInt(activity, 4));
                settingsBtn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        showApiKeyDialog(activity);
                    }
                });
                headerRow.addView(settingsBtn);
                panel.addView(headerRow);

                TextView subLabel = new TextView(activity);
                subLabel.setText(sl);
                subLabel.setTextSize(11);
                subLabel.setTextColor(TEXT_SUB);
                subLabel.setPadding(0, dpInt(activity, 8), 0, dpInt(activity, 16));
                panel.addView(subLabel);

                ScrollView scroll = new ScrollView(activity);
                scroll.setVerticalScrollBarEnabled(false);
                LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpInt(activity, 400));
                scroll.setLayoutParams(scrollParams);

                LinearLayout content = new LinearLayout(activity);
                content.setOrientation(LinearLayout.VERTICAL);

                content.addView(sectionTitle(activity, "触发规则"));
                JSONArray rules = getRules();
                content.addView(rulesCard(activity, rules, sk, ig, dialog, activity));

                content.addView(sectionTitle(activity, "回复设置"));
                content.addView(toggleItem(activity, "引用回复", isAutoQuote(sk), "auto_quote_" + sk));
                content.addView(toggleItem(activity, "忽略回复消息", isIgnoreReply(sk), "ignore_reply_" + sk));
                content.addView(toggleItem(activity, "回复自己消息", isReplySelf(sk), "reply_self_" + sk));
                content.addView(toggleItem(activity, "拍一拍回复", isPaiReply(sk), "pai_reply_" + sk));

                content.addView(sectionTitle(activity, "作用域管理"));
                content.addView(scopeManager(activity, sk, ig));

                scroll.addView(content);
                panel.addView(scroll);

                root.addView(panel);
                dialog.show();

                Window window = dialog.getWindow();
                if (window != null) {
                    window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                    window.setLayout(dpInt(activity, 320), ViewGroup.LayoutParams.WRAP_CONTENT);
                    window.setContentView(root);
                    window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
                    window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE | android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                }

            } catch (Throwable e) {
                error(e);
                Toast("界面加载失败");
            }
        }
    });
}

public void onMsg(Object msg) {
    try {
        if (msg == null) {
            return;
        }

        // ========== 拍一拍处理 ==========
        String[] paiPai = parsePaiPai(msg);
        if (paiPai != null) {
            String senderUin = paiPai[0];
            String targetUin = paiPai[1];
            
            // 不是拍自己，跳过
            if (!targetUin.equals(myUin)) {
                return;
            }
            
            // 构建作用域key
            String paiScopeKey = buildScopeKeyForMsg(msg);
            if (isEmpty(paiScopeKey)) {
                return;
            }
            
            // 检查作用域是否开启
            if (!isScopeEnabled(paiScopeKey)) {
                return;
            }
            
            // 检查拍一拍回复开关
            if (!isPaiReply(paiScopeKey)) {
                return;
            }
            
            // 限流检查
            if (!canPaiReply(paiScopeKey)) {
                return;
            }
            
            // 自己拍自己
            boolean isSelfPai = senderUin.equals(targetUin);
            if (isSelfPai && !isReplySelf(paiScopeKey)) {
                return;
            }
            
            // 调用AI回复拍一拍
            String reply = callAI("拍一拍", null, senderUin);
            if (isEmpty(reply)) {
                String errorReply = getErrorReply();
                if (!isEmpty(errorReply)) {
                    reply = errorReply;
                } else {
                    return;
                }
            }
            
            // 更新限流时间
            updatePaiReplyTime(paiScopeKey);
            
            Object contact = getContact(msg);
            boolean isGroup = isGroupMsg(msg);
            
            if (isGroup) {
                send(contact, "[atUin=" + senderUin + "] " + reply);
            } else {
                send(contact, reply);
            }
            return;
        }
        // ========== 拍一拍处理结束 ==========

        String scopeKey = buildScopeKeyForMsg(msg);
        
        if (isEmpty(scopeKey)) {
            return;
        }
        
        boolean scopeEnabled = isScopeEnabled(scopeKey);
        
        if (!scopeEnabled) {
            return;
        }

        int msgType = getMsgType(msg);
        
        if (msgType == 6 && isIgnoreReply(scopeKey)) {
            return;
        }

        boolean isSend = isSendByMe(msg);
        boolean isPrivate = scopeKey.startsWith("private_");
        
        if (isSend) {
            if (isPrivate) {
                return;
            }
            if (!isReplySelf(scopeKey)) {
                return;
            }
        }

        String rawText = getMsgText(msg);
        boolean hasAt = isAtMe(msg);
        String imgUrl = getImgUrl(msg);

        boolean matched = isPrivate;

        if (!isPrivate) {
            JSONArray rules = getRules();
            
            for (int i = 0; i < rules.length(); i++) {
                JSONObject rule = rules.optJSONObject(i);
                if (rule == null) continue;
                if (!rule.optBoolean("enabled", true)) continue;

                String mode = rule.optString("mode", "keyword");
                String trigger = rule.optString("trigger", "");

                if ("at".equals(mode)) {
                    if (hasAt) {
                        matched = true;
                        break;
                    }
                } else if ("keyword".equals(mode)) {
                    if (!isEmpty(trigger) && rawText.contains(trigger)) {
                        matched = true;
                        break;
                    }
                } else if ("regex".equals(mode)) {
                    if (!isEmpty(trigger)) {
                        try {
                            if (Pattern.compile(trigger).matcher(rawText).find()) {
                                matched = true;
                                break;
                            }
                        } catch (Throwable e) {
                            info("[onMsg] 正则匹配异常: " + e.getMessage());
                        }
                    }
                }
            }
        }

        if (!matched) {
            return;
        }

        String cleanText = removeAtTags(rawText);
        
        if (isEmpty(cleanText) && isEmpty(imgUrl)) {
            return;
        }

        String sender = getSenderUin(msg);
        
        String reply = callAI(cleanText, imgUrl, sender);
        
        if (isEmpty(reply)) {
            String errorReply = getErrorReply();
            if (isEmpty(errorReply)) {
                return;
            }
            reply = errorReply;
        }

        Object contact = getContact(msg);
        boolean isGroup = isGroupMsg(msg);

        if (isGroup) {
            if (isAutoQuote(scopeKey)) {
                long msgId = getMsgId(msg);
                reply(contact, msgId, reply);
            } else {
                send(contact, reply);
            }
        } else {
            send(contact, reply);
        }

    } catch (Throwable e) {
        info("[onMsg] 异常: " + e.getMessage());
        error(e);
    }
}

addItem("元启Ai自动回复", "showMainMenu");

zan("6110536", 20);
String apiKey = getApiKey();
if (isEmpty(apiKey)) {
    Toast("请配置元启API Key启用AI回复\nQQ交流群: 883640898");
}
checkJoinGroup();
