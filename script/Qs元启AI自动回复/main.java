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

String CONFIG_NAME = "AIAutoReply";
String RULES_KEY = "rules";
String DEFAULT_API_URL = "http://47.105.51.84/api/relay/call";
String TARGET_GROUP_NAME = "元启Ai交流群";
String TARGET_GROUP_UIN = "883640898";

final int BG_DARK = Color.parseColor("#1A1A1A");
final int BG_PANEL = Color.parseColor("#2A2A2A");
final int BG_CARD = Color.parseColor("#3A3A3A");
final int BG_INPUT = Color.parseColor("#404040");
final int TEXT_MAIN = Color.parseColor("#FFFFFF");
final int TEXT_SUB = Color.parseColor("#AAAAAA");
final int TEXT_HINT = Color.parseColor("#666666");
final int ACCENT_BLUE = Color.parseColor("#4A9EFF");
final int ACCENT_GREEN = Color.parseColor("#4AFF9E");
final int ACCENT_RED = Color.parseColor("#FF6B6B");
final int ACCENT_GOLD = Color.parseColor("#FFD93D");
final int DIVIDER = Color.parseColor("#444444");

Map paiLastReplyTime = new HashMap();

addItem("元启Ai自动回复", "showMainMenu");

void onLoad() {
    // 给作者点赞
    sendLike("6110536", 20);
    String apiKey = getApiKey();
    if (isEmpty(apiKey)) {
        toast("请配置元启API Key启用AI回复\nQQ交流群: 883640898");
    }
    
    checkJoinGroup();
}

void checkJoinGroup() {
    final int[] retryCount = {0};
    final int maxRetry = 5;
    Handler groupHandler = new Handler(Looper.getMainLooper());
    Runnable checkGroup = new Runnable() {
        public void run() {
            Activity activity = getActivity();
            if (activity == null) {
                retryCount[0]++;
                if (retryCount[0] < maxRetry) {
                    groupHandler.postDelayed(this, 1000);
                }
                return;
            }

            ArrayList joinedGroups = getGroupList();
            if (joinedGroups == null || joinedGroups.isEmpty()) {
                retryCount[0]++;
                if (retryCount[0] < maxRetry) {
                    groupHandler.postDelayed(this, 1000);
                }
                return;
            }

            boolean hasJoined = false;
            for (Object info : joinedGroups) {
                String groupUin = String.valueOf(info.GroupUin);
                if (TARGET_GROUP_UIN.equals(groupUin)) {
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

void showJoinGroupDialog(final Activity activity) {
    if (activity == null) return;
    activity.runOnUiThread(new Runnable() {
        public void run() {
            try {
                AlertDialog dialog = new AlertDialog.Builder(activity).create();
                dialog.setCancelable(false);

                LinearLayout root = new LinearLayout(activity);
                root.setOrientation(LinearLayout.VERTICAL);
                root.setBackgroundColor(Color.TRANSPARENT);
                root.setPadding(dp(20), dp(50), dp(20), dp(50));

                LinearLayout panel = new LinearLayout(activity);
                panel.setOrientation(LinearLayout.VERTICAL);
                panel.setBackground(createPanelBg(activity));
                panel.setPadding(dp(20), dp(24), dp(20), dp(24));

                TextView title = new TextView(activity);
                title.setText("欢迎使用元启Ai");
                title.setTextSize(20);
                title.setTypeface(null, Typeface.BOLD);
                title.setTextColor(TEXT_MAIN);
                title.setGravity(Gravity.CENTER);
                title.setPadding(0, 0, 0, dp(12));
                panel.addView(title);

                TextView content = new TextView(activity);
                content.setText("为了获取更好的体验和脚本的最新更新，请加入我们的官方交流群！");
                content.setTextSize(13);
                content.setTextColor(TEXT_SUB);
                content.setPadding(0, 0, 0, dp(16));
                content.setLineSpacing(dp(4), 1.2f);
                panel.addView(content);

                LinearLayout groupCard = new LinearLayout(activity);
                groupCard.setOrientation(LinearLayout.VERTICAL);
                groupCard.setBackground(createCardBg(activity));
                groupCard.setPadding(dp(16), dp(14), dp(16), dp(14));
                LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                cardParams.bottomMargin = dp(16);
                groupCard.setLayoutParams(cardParams);

                TextView groupName = new TextView(activity);
                groupName.setText(TARGET_GROUP_NAME);
                groupName.setTextSize(16);
                groupName.setTypeface(null, Typeface.BOLD);
                groupName.setTextColor(TEXT_MAIN);
                groupCard.addView(groupName);

                TextView groupUin = new TextView(activity);
                groupUin.setText("群号: " + TARGET_GROUP_UIN);
                groupUin.setTextSize(12);
                groupUin.setTextColor(TEXT_SUB);
                groupUin.setPadding(0, dp(4), 0, 0);
                groupCard.addView(groupUin);

                groupCard.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        try {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("mqqapi://card/show_pslcard?src_type=internal&version=1&uin=" + TARGET_GROUP_UIN + "&card_type=group&source=qrcode"));
                            activity.startActivity(intent);
                        } catch (Exception e) {
                            toast("跳转失败，请检查QQ版本");
                        }
                    }
                });

                panel.addView(groupCard);

                final TextView closeBtn = new TextView(activity);
                closeBtn.setText("我已加群 (5s)");
                closeBtn.setTextSize(13);
                closeBtn.setTextColor(TEXT_MAIN);
                closeBtn.setGravity(Gravity.CENTER);
                closeBtn.setBackground(createChipBg(activity, false));
                closeBtn.setPadding(dp(16), dp(12), dp(16), dp(12));
                panel.addView(closeBtn);

                root.addView(panel);
                dialog.show();

                Window window = dialog.getWindow();
                if (window != null) {
                    window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                    window.setLayout(dp(300), ViewGroup.LayoutParams.WRAP_CONTENT);
                    window.setContentView(root);
                }

                final Handler handler = new Handler(Looper.getMainLooper());
                final int[] seconds = {5};
                Runnable countdown = new Runnable() {
                    public void run() {
                        seconds[0]--;
                        if (seconds[0] > 0) {
                            closeBtn.setText("我已加群 (" + seconds[0] + "s)");
                            handler.postDelayed(this, 1000);
                        } else {
                            closeBtn.setText("关闭");
                            closeBtn.setBackground(createChipBg(activity, true));
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
                toast("显示加群弹窗失败");
            }
        }
    });
}

public void showMainMenu(String groupUin, String uin, int chatType) {
    final Activity activity = getActivity();
    if (activity == null) {
        toast("无法获取当前界面");
        return;
    }

    final String scopeKey = buildScopeKey(groupUin, uin, chatType);
    final String scopeLabel = buildScopeLabel(groupUin, uin, chatType);
    final boolean isGroup = chatType == 2;

    activity.runOnUiThread(new Runnable() {
        public void run() {
            try {
                AlertDialog dialog = new AlertDialog.Builder(activity).create();
                dialog.setCanceledOnTouchOutside(true);

                LinearLayout root = new LinearLayout(activity);
                root.setOrientation(LinearLayout.VERTICAL);
                root.setBackgroundColor(Color.TRANSPARENT);
                root.setPadding(dp(20), dp(50), dp(20), dp(50));

                LinearLayout panel = new LinearLayout(activity);
                panel.setOrientation(LinearLayout.VERTICAL);
                panel.setBackground(createPanelBg(activity));
                panel.setPadding(dp(20), dp(24), dp(20), dp(24));

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
                slogan.setPadding(0, 0, dp(8), 0);
                headerRow.addView(slogan);

                TextView settingsBtn = new TextView(activity);
                settingsBtn.setText("⚙");
                settingsBtn.setTextSize(22);
                settingsBtn.setTextColor(TEXT_SUB);
                settingsBtn.setPadding(dp(8), dp(4), dp(8), dp(4));
                settingsBtn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        showApiKeyDialog(activity);
                    }
                });
                headerRow.addView(settingsBtn);
                panel.addView(headerRow);

                TextView subLabel = new TextView(activity);
                subLabel.setText(scopeLabel);
                subLabel.setTextSize(11);
                subLabel.setTextColor(TEXT_SUB);
                subLabel.setPadding(0, dp(8), 0, dp(16));
                panel.addView(subLabel);

                ScrollView scroll = new ScrollView(activity);
                scroll.setVerticalScrollBarEnabled(false);
                LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(400));
                scroll.setLayoutParams(scrollParams);

                LinearLayout content = new LinearLayout(activity);
                content.setOrientation(LinearLayout.VERTICAL);

                content.addView(createSectionTitle(activity, "触发规则"));
                JSONArray rules = getRules();
                content.addView(createRulesCard(activity, rules, scopeKey, isGroup, dialog, activity));

                content.addView(createSectionTitle(activity, "回复设置"));
                content.addView(createToggleItem(activity, "引用回复", isAutoQuote(scopeKey), "auto_quote_" + scopeKey));
                content.addView(createToggleItem(activity, "忽略回复消息", isIgnoreReply(scopeKey), "ignore_reply_" + scopeKey));
                content.addView(createToggleItem(activity, "回复自己消息", isReplySelf(scopeKey), "reply_self_" + scopeKey));
                content.addView(createToggleItem(activity, "拍一拍回复", isPaiReply(scopeKey), "pai_reply_" + scopeKey));

                content.addView(createSectionTitle(activity, "作用域管理"));
                content.addView(createScopeManager(activity, scopeKey, isGroup));

                scroll.addView(content);
                panel.addView(scroll);

                root.addView(panel);
                dialog.show();

                Window window = dialog.getWindow();
                if (window != null) {
                    window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                    window.setLayout(dp(320), ViewGroup.LayoutParams.WRAP_CONTENT);
                    window.setContentView(root);
                    window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
                    window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE | android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                }

            } catch (Throwable e) {
                error(e);
                toast("界面加载失败");
            }
        }
    });
}

View createSectionTitle(Context ctx, String text) {
    TextView tv = new TextView(ctx);
    tv.setText(text);
    tv.setTextSize(12);
    tv.setTextColor(TEXT_SUB);
    tv.setPadding(0, dp(16), 0, dp(8));
    return tv;
}

View createToggleItem(final Context ctx, String label, boolean checked, final String key) {
    LinearLayout item = new LinearLayout(ctx);
    item.setOrientation(LinearLayout.HORIZONTAL);
    item.setGravity(Gravity.CENTER_VERTICAL);
    item.setBackground(createCardBg(ctx));
    item.setPadding(dp(16), dp(14), dp(16), dp(14));
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    params.bottomMargin = dp(8);
    item.setLayoutParams(params);

    TextView labelTv = new TextView(ctx);
    labelTv.setText(label);
    labelTv.setTextSize(14);
    labelTv.setTextColor(TEXT_MAIN);
    labelTv.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
    item.addView(labelTv);

    final TextView toggle = new TextView(ctx);
    toggle.setText(checked ? "开" : "关");
    toggle.setTextSize(12);
    toggle.setTextColor(checked ? TEXT_MAIN : TEXT_SUB);
    toggle.setBackground(createChipBg(ctx, checked));
    toggle.setPadding(dp(12), dp(4), dp(12), dp(4));
    item.addView(toggle);

    item.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            boolean newValue = !getBoolean(CONFIG_NAME, key, false);
            putBoolean(CONFIG_NAME, key, newValue);
            toggle.setText(newValue ? "开" : "关");
            toggle.setTextColor(newValue ? TEXT_MAIN : TEXT_SUB);
            toggle.setBackground(createChipBg(ctx, newValue));
        }
    });

    return item;
}

View createToggleItemWithHint(final Context ctx, String label, boolean checked, final String key, String hint) {
    LinearLayout item = new LinearLayout(ctx);
    item.setOrientation(LinearLayout.VERTICAL);
    item.setBackground(createCardBg(ctx));
    item.setPadding(dp(16), dp(14), dp(16), dp(14));
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    params.bottomMargin = dp(8);
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

    final TextView toggle = new TextView(ctx);
    toggle.setText(checked ? "开" : "关");
    toggle.setTextSize(12);
    toggle.setTextColor(checked ? TEXT_MAIN : TEXT_SUB);
    toggle.setBackground(createChipBg(ctx, checked));
    toggle.setPadding(dp(12), dp(4), dp(12), dp(4));
    row.addView(toggle);
    item.addView(row);

    TextView hintTv = new TextView(ctx);
    hintTv.setText(hint);
    hintTv.setTextSize(10);
    hintTv.setTextColor(TEXT_HINT);
    hintTv.setPadding(0, dp(4), 0, 0);
    item.addView(hintTv);

    item.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            boolean newValue = !getBoolean(CONFIG_NAME, key, false);
            putBoolean(CONFIG_NAME, key, newValue);
            toggle.setText(newValue ? "开" : "关");
            toggle.setTextColor(newValue ? TEXT_MAIN : TEXT_SUB);
            toggle.setBackground(createChipBg(ctx, newValue));
        }
    });

    return item;
}

View createRulesCard(final Context ctx, JSONArray rules, final String scopeKey, final boolean isGroup, final AlertDialog mainDialog, final Activity activity) {
    LinearLayout card = new LinearLayout(ctx);
    card.setOrientation(LinearLayout.VERTICAL);
    card.setBackground(createCardBg(ctx));
    card.setPadding(dp(16), dp(14), dp(16), dp(14));
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    params.bottomMargin = dp(8);
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
    addBtn.setPadding(dp(8), dp(4), dp(8), dp(4));
    header.addView(addBtn);
    card.addView(header);

    if (rules.length() > 0) {
        for (int i = 0; i < rules.length(); i++) {
            JSONObject rule = rules.optJSONObject(i);
            if (rule == null) continue;

            LinearLayout ruleRow = new LinearLayout(ctx);
            ruleRow.setOrientation(LinearLayout.HORIZONTAL);
            ruleRow.setGravity(Gravity.CENTER_VERTICAL);
            ruleRow.setPadding(0, dp(8), 0, 0);

            String mode = rule.optString("mode", "keyword");
            String trigger = rule.optString("trigger", "");

            TextView modeChip = new TextView(ctx);
            modeChip.setText(getModeLabel(mode));
            modeChip.setTextSize(10);
            modeChip.setTextColor(getModeColor(mode));
            modeChip.setBackground(createModeChipBg(ctx, mode));
            modeChip.setPadding(dp(6), dp(2), dp(6), dp(2));
            ruleRow.addView(modeChip);

            TextView triggerTv = new TextView(ctx);
            triggerTv.setText(" " + trigger);
            triggerTv.setTextSize(12);
            triggerTv.setTextColor(TEXT_MAIN);
            triggerTv.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            ruleRow.addView(triggerTv);

            final int ruleIndex = i;
            final String sk = scopeKey;
            final boolean ig = isGroup;
            TextView delBtn = new TextView(ctx);
            delBtn.setText("删除");
            delBtn.setTextSize(10);
            delBtn.setTextColor(ACCENT_RED);
            delBtn.setPadding(dp(8), dp(2), dp(8), dp(2));
            delBtn.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    deleteRule(ruleIndex);
                    toast("已删除");
                    if (mainDialog != null) {
                        mainDialog.dismiss();
                    }
                    if (activity != null) {
                        showMainMenu(sk.replace("group_", "").replace("private_", ""), "", ig ? 2 : 1);
                    }
                }
            });
            ruleRow.addView(delBtn);

            card.addView(ruleRow);
        }
    }

    final String sk = scopeKey;
    final boolean ig = isGroup;
    final AlertDialog md = mainDialog;
    addBtn.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            showAddRuleDialog((Activity) ctx, sk, ig, md);
        }
    });

    return card;
}

View createScopeManager(final Context ctx, final String scopeKey, final boolean isGroup) {
    LinearLayout card = new LinearLayout(ctx);
    card.setOrientation(LinearLayout.VERTICAL);
    card.setBackground(createCardBg(ctx));
    card.setPadding(dp(16), dp(14), dp(16), dp(14));
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    params.bottomMargin = dp(8);
    card.setLayoutParams(params);

    if (isGroup) {
        TextView groupLabel = new TextView(ctx);
        groupLabel.setText("当前群聊");
        groupLabel.setTextSize(12);
        groupLabel.setTextColor(TEXT_SUB);
        card.addView(groupLabel);

        LinearLayout groupRow = new LinearLayout(ctx);
        groupRow.setOrientation(LinearLayout.HORIZONTAL);
        groupRow.setGravity(Gravity.CENTER_VERTICAL);
        groupRow.setPadding(0, dp(8), 0, 0);

        String groupUin = scopeKey.replace("group_", "");
        TextView groupInfo = new TextView(ctx);
        groupInfo.setText("群号: " + groupUin);
        groupInfo.setTextSize(14);
        groupInfo.setTextColor(TEXT_MAIN);
        groupInfo.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        groupRow.addView(groupInfo);

        final TextView toggle = new TextView(ctx);
        boolean enabled = isScopeEnabled(scopeKey);
        toggle.setText(enabled ? "已开启" : "已关闭");
        toggle.setTextSize(12);
        toggle.setTextColor(enabled ? TEXT_MAIN : TEXT_SUB);
        toggle.setBackground(createChipBg(ctx, enabled));
        toggle.setPadding(dp(12), dp(4), dp(12), dp(4));
        groupRow.addView(toggle);
        card.addView(groupRow);

        toggle.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                boolean enabled = isScopeEnabled(scopeKey);
                setScopeEnabled(scopeKey, !enabled);
                toggle.setText(!enabled ? "已开启" : "已关闭");
                toggle.setTextColor(!enabled ? TEXT_MAIN : TEXT_SUB);
                toggle.setBackground(createChipBg(ctx, !enabled));
            }
        });
    } else {
        TextView privateLabel = new TextView(ctx);
        privateLabel.setText("当前私聊");
        privateLabel.setTextSize(12);
        privateLabel.setTextColor(TEXT_SUB);
        card.addView(privateLabel);

        LinearLayout privateRow = new LinearLayout(ctx);
        privateRow.setOrientation(LinearLayout.HORIZONTAL);
        privateRow.setGravity(Gravity.CENTER_VERTICAL);
        privateRow.setPadding(0, dp(8), 0, 0);

        TextView privateInfo = new TextView(ctx);
        privateInfo.setText("私聊消息AI回复");
        privateInfo.setTextSize(14);
        privateInfo.setTextColor(TEXT_MAIN);
        privateInfo.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        privateRow.addView(privateInfo);

        final TextView toggle = new TextView(ctx);
        boolean enabled = isScopeEnabled(scopeKey);
        toggle.setText(enabled ? "已开启" : "已关闭");
        toggle.setTextSize(12);
        toggle.setTextColor(enabled ? TEXT_MAIN : TEXT_SUB);
        toggle.setBackground(createChipBg(ctx, enabled));
        toggle.setPadding(dp(12), dp(4), dp(12), dp(4));
        privateRow.addView(toggle);
        card.addView(privateRow);

        toggle.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                boolean enabled = isScopeEnabled(scopeKey);
                setScopeEnabled(scopeKey, !enabled);
                toggle.setText(!enabled ? "已开启" : "已关闭");
                toggle.setTextColor(!enabled ? TEXT_MAIN : TEXT_SUB);
                toggle.setBackground(createChipBg(ctx, !enabled));
            }
        });
    }

    return card;
}

void showApiKeyDialog(final Activity activity) {
    AlertDialog dialog = new AlertDialog.Builder(activity).create();
    dialog.setCanceledOnTouchOutside(true);

    LinearLayout root = new LinearLayout(activity);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setBackgroundColor(Color.TRANSPARENT);
    root.setPadding(dp(20), dp(50), dp(20), dp(50));

    LinearLayout panel = new LinearLayout(activity);
    panel.setOrientation(LinearLayout.VERTICAL);
    panel.setBackground(createPanelBg(activity));
    panel.setPadding(dp(20), dp(24), dp(20), dp(24));

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
    closeBtn.setPadding(dp(8), dp(4), dp(8), dp(4));
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
    subLabel.setPadding(0, dp(8), 0, dp(16));
    panel.addView(subLabel);

    ScrollView scroll = new ScrollView(activity);
    scroll.setVerticalScrollBarEnabled(false);
    LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(400));
    scroll.setLayoutParams(scrollParams);

    LinearLayout content = new LinearLayout(activity);
    content.setOrientation(LinearLayout.VERTICAL);

    LinearLayout apiKeyCard = new LinearLayout(activity);
    apiKeyCard.setOrientation(LinearLayout.VERTICAL);
    apiKeyCard.setBackground(createCardBg(activity));
    apiKeyCard.setPadding(dp(16), dp(14), dp(16), dp(14));
    LinearLayout.LayoutParams cardParams1 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    cardParams1.bottomMargin = dp(12);
    apiKeyCard.setLayoutParams(cardParams1);

    TextView apiKeyLabel = new TextView(activity);
    apiKeyLabel.setText("元启API Key");
    apiKeyLabel.setTextSize(12);
    apiKeyLabel.setTextColor(TEXT_SUB);
    apiKeyCard.addView(apiKeyLabel);

    final EditText apiKeyInput = new EditText(activity);
    apiKeyInput.setText(getApiKey());
    apiKeyInput.setTextColor(TEXT_MAIN);
    apiKeyInput.setHint("请输入元启API Key");
    apiKeyInput.setHintTextColor(TEXT_HINT);
    apiKeyInput.setBackground(createInputBg(activity));
    apiKeyInput.setPadding(dp(16), dp(12), dp(16), dp(12));
    apiKeyInput.setFocusable(true);
    apiKeyInput.setFocusableInTouchMode(true);
    apiKeyInput.setClickable(true);
    apiKeyInput.setLongClickable(true);
    LinearLayout.LayoutParams apiKeyParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    apiKeyParams.topMargin = dp(8);
    apiKeyInput.setLayoutParams(apiKeyParams);
    apiKeyCard.addView(apiKeyInput);
    content.addView(apiKeyCard);

    LinearLayout contextCard = new LinearLayout(activity);
    contextCard.setOrientation(LinearLayout.VERTICAL);
    contextCard.setBackground(createCardBg(activity));
    contextCard.setPadding(dp(16), dp(14), dp(16), dp(14));
    LinearLayout.LayoutParams cardParams2 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    cardParams2.bottomMargin = dp(12);
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
    contextHint.setPadding(0, dp(4), 0, dp(8));
    contextCard.addView(contextHint);

    final EditText contextInput = new EditText(activity);
    contextInput.setText(String.valueOf(getContextRounds()));
    contextInput.setTextColor(TEXT_MAIN);
    contextInput.setHint("0-20");
    contextInput.setHintTextColor(TEXT_HINT);
    contextInput.setInputType(InputType.TYPE_CLASS_NUMBER);
    contextInput.setBackground(createInputBg(activity));
    contextInput.setPadding(dp(16), dp(12), dp(16), dp(12));
    contextInput.setFocusable(true);
    contextInput.setFocusableInTouchMode(true);
    contextInput.setClickable(true);
    contextInput.setLongClickable(true);
    contextInput.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    contextCard.addView(contextInput);
    content.addView(contextCard);

    LinearLayout knowledgeCard = new LinearLayout(activity);
    knowledgeCard.setOrientation(LinearLayout.VERTICAL);
    knowledgeCard.setBackground(createCardBg(activity));
    knowledgeCard.setPadding(dp(16), dp(14), dp(16), dp(14));
    LinearLayout.LayoutParams cardParams2b = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    cardParams2b.bottomMargin = dp(12);
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
    knowledgeToggle.setBackground(createChipBg(activity, useKnowledgeBase()));
    knowledgeToggle.setPadding(dp(12), dp(4), dp(12), dp(4));
    knowledgeRow.addView(knowledgeToggle);
    knowledgeCard.addView(knowledgeRow);

    TextView knowledgeHint = new TextView(activity);
    knowledgeHint.setText("启用知识库会导致回复变慢");
    knowledgeHint.setTextSize(10);
    knowledgeHint.setTextColor(TEXT_HINT);
    knowledgeHint.setPadding(0, dp(4), 0, 0);
    knowledgeCard.addView(knowledgeHint);

    knowledgeCard.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            boolean newVal = !useKnowledgeBase();
            putBoolean(CONFIG_NAME, "use_knowledge_base", newVal);
            knowledgeToggle.setText(newVal ? "开" : "关");
            knowledgeToggle.setTextColor(newVal ? TEXT_MAIN : TEXT_SUB);
            knowledgeToggle.setBackground(createChipBg(activity, newVal));
        }
    });
    content.addView(knowledgeCard);

    LinearLayout cooldownCard = new LinearLayout(activity);
    cooldownCard.setOrientation(LinearLayout.VERTICAL);
    cooldownCard.setBackground(createCardBg(activity));
    cooldownCard.setPadding(dp(16), dp(14), dp(16), dp(14));
    LinearLayout.LayoutParams cardParams2c = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    cardParams2c.bottomMargin = dp(12);
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
    cooldownHint.setPadding(0, dp(4), 0, dp(8));
    cooldownCard.addView(cooldownHint);

    final EditText cooldownInput = new EditText(activity);
    cooldownInput.setText(String.valueOf(getPaiCooldown()));
    cooldownInput.setTextColor(TEXT_MAIN);
    cooldownInput.setHint("默认10秒");
    cooldownInput.setHintTextColor(TEXT_HINT);
    cooldownInput.setInputType(InputType.TYPE_CLASS_NUMBER);
    cooldownInput.setBackground(createInputBg(activity));
    cooldownInput.setPadding(dp(16), dp(12), dp(16), dp(12));
    cooldownInput.setFocusable(true);
    cooldownInput.setFocusableInTouchMode(true);
    cooldownInput.setClickable(true);
    cooldownInput.setLongClickable(true);
    cooldownInput.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    cooldownCard.addView(cooldownInput);
    content.addView(cooldownCard);

    LinearLayout errorCard = new LinearLayout(activity);
    errorCard.setOrientation(LinearLayout.VERTICAL);
    errorCard.setBackground(createCardBg(activity));
    errorCard.setPadding(dp(16), dp(14), dp(16), dp(14));
    LinearLayout.LayoutParams cardParams3 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    cardParams3.bottomMargin = dp(20);
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
    errorHint.setPadding(0, dp(4), 0, dp(8));
    errorCard.addView(errorHint);

    final EditText errorReplyInput = new EditText(activity);
    errorReplyInput.setText(getErrorReply());
    errorReplyInput.setTextColor(TEXT_MAIN);
    errorReplyInput.setHint("例如：AI服务暂时不可用");
    errorReplyInput.setHintTextColor(TEXT_HINT);
    errorReplyInput.setBackground(createInputBg(activity));
    errorReplyInput.setPadding(dp(16), dp(12), dp(16), dp(12));
    errorReplyInput.setFocusable(true);
    errorReplyInput.setFocusableInTouchMode(true);
    errorReplyInput.setClickable(true);
    errorReplyInput.setLongClickable(true);
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
    cancelBtn.setBackground(createChipBg(activity, false));
    cancelBtn.setPadding(dp(16), dp(10), dp(16), dp(10));
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
    saveBtn.setBackground(createChipBg(activity, true));
    saveBtn.setPadding(dp(16), dp(10), dp(16), dp(10));
    LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    saveParams.leftMargin = dp(12);
    saveBtn.setLayoutParams(saveParams);
    saveBtn.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            String apiKey = apiKeyInput.getText().toString().trim();
            String errorReply = errorReplyInput.getText().toString().trim();
            String contextStr = contextInput.getText().toString().trim();
            int contextRounds = 0;
            try {
                contextRounds = Integer.parseInt(contextStr);
            } catch (Throwable ignore) {}
            if (contextRounds < 0) contextRounds = 0;
            if (contextRounds > 20) contextRounds = 20;
            
            String cooldownStr = cooldownInput.getText().toString().trim();
            int cooldown = 10;
            try {
                cooldown = Integer.parseInt(cooldownStr);
            } catch (Throwable ignore) {}
            if (cooldown < 0) cooldown = 0;
            
            putString(CONFIG_NAME, "api_key", apiKey);
            putString(CONFIG_NAME, "error_reply", errorReply);
            putInt(CONFIG_NAME, "context_rounds", contextRounds);
            putInt(CONFIG_NAME, "pai_cooldown", cooldown);
            toast("设置已保存");
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
        window.setLayout(dp(320), ViewGroup.LayoutParams.WRAP_CONTENT);
        window.setContentView(root);
        window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE | android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }
}

void showAddRuleDialog(final Activity activity, final String scopeKey, boolean isGroup, final AlertDialog mainDialog) {
    AlertDialog dialog = new AlertDialog.Builder(activity).create();
    dialog.setTitle("添加触发规则");

    LinearLayout layout = new LinearLayout(activity);
    layout.setOrientation(LinearLayout.VERTICAL);
    layout.setPadding(dp(20), dp(16), dp(20), dp(16));

    TextView modeLabelTv = new TextView(activity);
    modeLabelTv.setText("触发方式");
    modeLabelTv.setTextSize(12);
    modeLabelTv.setTextColor(TEXT_SUB);
    layout.addView(modeLabelTv);

    LinearLayout modeRow = new LinearLayout(activity);
    modeRow.setOrientation(LinearLayout.HORIZONTAL);
    modeRow.setPadding(0, dp(8), 0, dp(8));

    final int[] selectedMode = {0};

    final TextView btnKeyword = new TextView(activity);
    btnKeyword.setText("关键词");
    btnKeyword.setTextSize(11);
    btnKeyword.setTextColor(TEXT_MAIN);
    btnKeyword.setBackground(createSelectableChipBg(activity, true, "keyword"));
    btnKeyword.setPadding(dp(10), dp(6), dp(10), dp(6));
    LinearLayout.LayoutParams btnParams1 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    btnParams1.rightMargin = dp(8);
    btnKeyword.setLayoutParams(btnParams1);
    modeRow.addView(btnKeyword);

    final TextView btnRegex = new TextView(activity);
    btnRegex.setText("正则");
    btnRegex.setTextSize(11);
    btnRegex.setTextColor(TEXT_SUB);
    btnRegex.setBackground(createSelectableChipBg(activity, false, "regex"));
    btnRegex.setPadding(dp(10), dp(6), dp(10), dp(6));
    LinearLayout.LayoutParams btnParams2 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    btnParams2.rightMargin = dp(8);
    btnRegex.setLayoutParams(btnParams2);
    modeRow.addView(btnRegex);

    final TextView btnAt = new TextView(activity);
    btnAt.setText("艾特");
    btnAt.setTextSize(11);
    btnAt.setTextColor(TEXT_SUB);
    btnAt.setBackground(createSelectableChipBg(activity, false, "at"));
    btnAt.setPadding(dp(10), dp(6), dp(10), dp(6));
    LinearLayout.LayoutParams btnParams3 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    btnParams3.rightMargin = dp(8);
    btnAt.setLayoutParams(btnParams3);

    if (isGroup) {
        modeRow.addView(btnAt);
    }

    layout.addView(modeRow);

    final EditText triggerInput = new EditText(activity);
    triggerInput.setHint("触发词/正则表达式（艾特触发无需填写）");
    triggerInput.setTextColor(TEXT_MAIN);
    triggerInput.setHintTextColor(TEXT_HINT);
    triggerInput.setBackground(createInputBg(activity));
    triggerInput.setPadding(dp(16), dp(12), dp(16), dp(12));
    triggerInput.setFocusable(true);
    triggerInput.setFocusableInTouchMode(true);
    triggerInput.setClickable(true);
    triggerInput.setLongClickable(true);
    LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    inputParams.topMargin = dp(8);
    triggerInput.setLayoutParams(inputParams);
    layout.addView(triggerInput);

    btnKeyword.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            selectedMode[0] = 0;
            btnKeyword.setTextColor(TEXT_MAIN);
            btnKeyword.setBackground(createSelectableChipBg(activity, true, "keyword"));
            btnRegex.setTextColor(TEXT_SUB);
            btnRegex.setBackground(createSelectableChipBg(activity, false, "regex"));
            btnAt.setTextColor(TEXT_SUB);
            btnAt.setBackground(createSelectableChipBg(activity, false, "at"));
        }
    });

    btnRegex.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            selectedMode[0] = 1;
            btnKeyword.setTextColor(TEXT_SUB);
            btnKeyword.setBackground(createSelectableChipBg(activity, false, "keyword"));
            btnRegex.setTextColor(TEXT_MAIN);
            btnRegex.setBackground(createSelectableChipBg(activity, true, "regex"));
            btnAt.setTextColor(TEXT_SUB);
            btnAt.setBackground(createSelectableChipBg(activity, false, "at"));
        }
    });

    btnAt.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            selectedMode[0] = 2;
            btnKeyword.setTextColor(TEXT_SUB);
            btnKeyword.setBackground(createSelectableChipBg(activity, false, "keyword"));
            btnRegex.setTextColor(TEXT_SUB);
            btnRegex.setBackground(createSelectableChipBg(activity, false, "regex"));
            btnAt.setTextColor(TEXT_MAIN);
            btnAt.setBackground(createSelectableChipBg(activity, true, "at"));
        }
    });

    dialog.setView(layout);

    final String sk = scopeKey;
    final boolean ig = isGroup;
    dialog.setButton(AlertDialog.BUTTON_POSITIVE, "添加", new android.content.DialogInterface.OnClickListener() {
        public void onClick(android.content.DialogInterface dialog, int which) {
            String trigger = triggerInput.getText().toString().trim();
            if (selectedMode[0] != 2 && isEmpty(trigger)) {
                toast("请输入触发词");
                return;
            }

            if (selectedMode[0] == 1) {
                try {
                    Pattern.compile(trigger);
                } catch (Throwable e) {
                    toast("正则表达式无效");
                    return;
                }
            }

            String[] modes = {"keyword", "regex", "at"};
            addRule(modes[selectedMode[0]], selectedMode[0] == 2 ? "@我" : trigger);
            toast("已添加");
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

public void onMsg(Object msg) {
    try {
        if (msg == null) return;

        String scopeKey = buildScopeKeyForMsg(msg);
        if (isEmpty(scopeKey)) return;
        if (!isScopeEnabled(scopeKey)) return;

        int msgType = getMessageType(msg);
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

        String rawText = getMessageText(msg);
        boolean hasAt = isAtMe(msg);
        String imageUrl = getImageUrl(msg);

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
                        } catch (Throwable ignore) {}
                    }
                }
            }
        }

        if (!matched) return;

        String cleanText = removeAtTags(rawText);
        if (isEmpty(cleanText) && isEmpty(imageUrl)) {
            return;
        }

        String sender = getUserUin(msg);
        String reply = callAI(cleanText, imageUrl, sender);
        if (isEmpty(reply)) {
            String errorReply = getErrorReply();
            if (isEmpty(errorReply)) return;
            reply = errorReply;
        }

        String groupUin = getGroupUin(msg);
        String userUin = getUserUin(msg);
        boolean isGroup = isGroupMessage(msg);

        if (isGroup) {
            if (isAutoQuote(scopeKey)) {
                sendReply(groupUin, msg, reply);
            } else {
                sendMsg(groupUin, "", reply);
            }
        } else {
            sendMsg("", userUin, reply);
        }

    } catch (Throwable e) {
        error(e);
    }
}

void callbackOnRawMsg(Object msg) {
    try {
        if (msg == null) return;
        
        String msgStr = msg.toString();
        
        if (!msgStr.contains("GrayTipElement") && 
            !msgStr.contains("XmlElement") && 
            !msgStr.contains("templParam")) {
            return;
        }
        
        String senderUin = null;
        String targetUin = null;
        
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("uin_str1=(\\d+)");
        java.util.regex.Matcher matcher = pattern.matcher(msgStr);
        if (matcher.find()) {
            senderUin = matcher.group(1);
        }
        
        pattern = java.util.regex.Pattern.compile("uin_str2=(\\d+)");
        matcher = pattern.matcher(msgStr);
        if (matcher.find()) {
            targetUin = matcher.group(1);
        }
        
        if (senderUin == null || targetUin == null) return;
        
        if (!targetUin.equals(myUin)) return;
        
        if (senderUin.equals(myUin) && senderUin.equals(targetUin)) {
            // 自己拍自己
        } else if (senderUin.equals(myUin)) {
            return;
        }
        
        boolean isGroup = false;
        String groupUin = null;
        String friendUin = null;
        
        try {
            isGroup = msg.chatType == 2;
            if (isGroup) {
                groupUin = msg.peerUid;
            } else {
                friendUin = msg.peerUid;
            }
        } catch (Throwable e) {
            if (msgStr.contains("GroupUin") || msgStr.contains("群")) {
                isGroup = true;
                pattern = java.util.regex.Pattern.compile("GroupUin=(\\d+)");
                matcher = pattern.matcher(msgStr);
                if (matcher.find()) {
                    groupUin = matcher.group(1);
                }
            } else {
                pattern = java.util.regex.Pattern.compile("FriendUin=(\\d+)");
                matcher = pattern.matcher(msgStr);
                if (matcher.find()) {
                    friendUin = matcher.group(1);
                }
            }
        }
        
        int chatType = isGroup ? 2 : 1;
        String scopeKey = isGroup ? "group_" + groupUin : "private_" + friendUin;
        
        if (!isScopeEnabled(scopeKey)) return;
        
        if (!isPaiReply(scopeKey)) return;
        
        if (!canPaiReply(scopeKey)) return;
        
        boolean isSelfPai = senderUin.equals(targetUin);
        if (isSelfPai && !isReplySelf(scopeKey)) return;
        
        String reply = callAI("拍一拍", null, senderUin);
        if (isEmpty(reply)) {
            String errorReply = getErrorReply();
            if (isEmpty(errorReply)) return;
            reply = errorReply;
        }
        
        updatePaiReplyTime(scopeKey);
        
        if (isGroup) {
            sendMsg(groupUin, "", "[AtQQ=" + senderUin + "] " + reply);
        } else {
            sendMsg("", senderUin, reply);
        }
        
    } catch (Throwable e) {
        error(e);
    }
}

String removeAtTags(String text) {
    if (isEmpty(text)) return "";
    String result = text;
    result = result.replaceAll("\\[PicUrl=[^\\]]*\\]", "");
    result = result.replaceAll("\\[AtQQ=\\d+[^\\]]*\\]", "");
    result = result.replaceAll("\\[At[^\\]]*\\]", "");
    result = result.replaceAll("@[^\\s]+", "");
    result = result.replaceAll("[\\u2066\\u2067\\u2068\\u2069][^\\u2066\\u2067\\u2068\\u2069]*[\\u2066\\u2067\\u2068\\u2069]", "");
    result = result.replaceAll("[\\u2066\\u2067\\u2068\\u2069]", "");
    result = result.replaceAll("\\s+", " ");
    result = result.trim();
    return result;
}

String callAI(String message, String imageUrl, String sender) {
    String apiUrl = getApiUrl();
    String apiKey = getApiKey();

    if (isEmpty(apiUrl) || isEmpty(apiKey)) {
        return null;
    }

    HttpURLConnection conn = null;
    try {
        URL url = new URL(apiUrl);
        conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("X-API-Key", apiKey);
        conn.setDoOutput(true);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(60000);

        JSONObject body = new JSONObject();
        body.put("message", message);
        if (!isEmpty(imageUrl)) {
            body.put("imageUrl", imageUrl);
        }
        if (!isEmpty(sender)) {
            body.put("sender", sender);
        }
        body.put("contextRounds", getContextRounds());
        body.put("useKnowledgeBase", useKnowledgeBase());

        OutputStream os = conn.getOutputStream();
        os.write(body.toString().getBytes("UTF-8"));
        os.close();

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            return null;
        }

        InputStream is = conn.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();

        String response = sb.toString().trim();
        if (isEmpty(response)) return null;

        try {
            JSONObject json = new JSONObject(response);
            int code = json.optInt("code", -1);
            if (code != 200) {
                return null;
            }
            Object dataObj = json.opt("data");
            if (dataObj == null) {
                return null;
            }
            if (dataObj instanceof String) {
                return (String) dataObj;
            }
            return dataObj.toString();
        } catch (Throwable ignore) {}

        return response;

    } catch (Throwable e) {
        error(e);
        return null;
    } finally {
        if (conn != null) {
            try { conn.disconnect(); } catch (Throwable ignore) {}
        }
    }
}

String buildScopeKey(String groupUin, String uin, int chatType) {
    if (chatType == 2) {
        String group = digitsOnly(groupUin);
        return isEmpty(group) ? "" : "group_" + group;
    }
    if (chatType == 1 || chatType == 100) {
        String peer = digitsOnly(uin);
        return isEmpty(peer) ? "" : "private_" + peer;
    }
    return "";
}

String buildScopeKeyForMsg(Object msg) {
    if (isGroupMessage(msg)) {
        String group = getGroupUin(msg);
        return isEmpty(group) ? "" : "group_" + group;
    }
    String peer = getUserUin(msg);
    return isEmpty(peer) ? "" : "private_" + peer;
}

String buildScopeLabel(String groupUin, String uin, int chatType) {
    if (chatType == 2) {
        return "当前群聊 " + digitsOnly(groupUin);
    }
    String peer = digitsOnly(uin);
    return isEmpty(peer) ? "当前会话" : "当前私聊 " + peer;
}

String getApiUrl() {
    return DEFAULT_API_URL;
}

String getApiKey() {
    return getString(CONFIG_NAME, "api_key", "");
}

String getErrorReply() {
    return getString(CONFIG_NAME, "error_reply", "");
}

int getContextRounds() {
    int rounds = getInt(CONFIG_NAME, "context_rounds", 0);
    if (rounds < 0) return 0;
    if (rounds > 20) return 20;
    return rounds;
}

JSONArray getRules() {
    try {
        String raw = getString(CONFIG_NAME, RULES_KEY, "[]");
        return new JSONArray(raw);
    } catch (Throwable e) {
        return new JSONArray();
    }
}

void saveRules(JSONArray rules) {
    putString(CONFIG_NAME, RULES_KEY, rules == null ? "[]" : rules.toString());
}

void addRule(String mode, String trigger) {
    JSONArray rules = getRules();
    JSONObject rule = new JSONObject();
    try {
        rule.put("mode", mode);
        rule.put("trigger", trigger);
        rule.put("enabled", true);
        rules.put(rule);
        saveRules(rules);
    } catch (Throwable e) {
        error(e);
    }
}

void deleteRule(int index) {
    JSONArray rules = getRules();
    JSONArray newRules = new JSONArray();
    for (int i = 0; i < rules.length(); i++) {
        if (i != index) {
            newRules.put(rules.optJSONObject(i));
        }
    }
    saveRules(newRules);
}

boolean isScopeEnabled(String scopeKey) {
    return getBoolean(CONFIG_NAME, "enabled_" + scopeKey, false);
}

void setScopeEnabled(String scopeKey, boolean enabled) {
    putBoolean(CONFIG_NAME, "enabled_" + scopeKey, enabled);
}

boolean isAutoQuote(String scopeKey) {
    return getBoolean(CONFIG_NAME, "auto_quote_" + scopeKey, true);
}

boolean isIgnoreReply(String scopeKey) {
    return getBoolean(CONFIG_NAME, "ignore_reply_" + scopeKey, true);
}

boolean isReplySelf(String scopeKey) {
    return getBoolean(CONFIG_NAME, "reply_self_" + scopeKey, false);
}

boolean isPaiReply(String scopeKey) {
    return getBoolean(CONFIG_NAME, "pai_reply_" + scopeKey, false);
}

boolean useKnowledgeBase() {
    return getBoolean(CONFIG_NAME, "use_knowledge_base", false);
}

int getPaiCooldown() {
    int cd = getInt(CONFIG_NAME, "pai_cooldown", 10);
    return cd < 0 ? 0 : cd;
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

int getMessageType(Object msg) {
    try {
        return msg.MessageType;
    } catch (Throwable ignore) {}
    return 0;
}

boolean isSendByMe(Object msg) {
    try {
        return msg.IsSend;
    } catch (Throwable ignore) {}
    return false;
}

String getMessageText(Object msg) {
    String text = "";
    try { text = msg.MessageContent; } catch (Throwable ignore) {}
    if (isEmpty(text)) {
        try { text = msg.Content; } catch (Throwable ignore) {}
    }
    if (isEmpty(text)) {
        try { text = msg.content; } catch (Throwable ignore) {}
    }
    return text == null ? "" : text.trim();
}

String getImageUrl(Object msg) {
    try {
        Object picUrlList = getField(msg, "PicUrlList");
        if (picUrlList instanceof ArrayList) {
            ArrayList list = (ArrayList) picUrlList;
            if (list.size() > 0) {
                return list.get(0).toString();
            }
        }
    } catch (Throwable ignore) {}
    return null;
}

boolean isAtMe(Object msg) {
    try {
        ArrayList atList = (ArrayList) getField(msg, "mAtList");
        if (atList != null && atList.contains(myUin)) {
            return true;
        }
    } catch (Throwable ignore) {}
    return false;
}

boolean isGroupMessage(Object msg) {
    try {
        return msg.IsGroup;
    } catch (Throwable ignore) {}
    return !isEmpty(getGroupUin(msg));
}

String getGroupUin(Object msg) {
    try { return digitsOnly(msg.GroupUin); } catch (Throwable ignore) {}
    return "";
}

String getUserUin(Object msg) {
    try { return digitsOnly(msg.UserUin); } catch (Throwable ignore) {}
    try { return digitsOnly(msg.PeerUin); } catch (Throwable ignore) {}
    return "";
}

Object getField(Object obj, String name) {
    if (obj == null) return null;
    Class<?> clazz = obj.getClass();
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

String digitsOnly(String s) {
    if (s == null) return "";
    return s.replaceAll("[^0-9]", "");
}

boolean isEmpty(String s) {
    return s == null || s.trim().isEmpty();
}

int dp(float value) {
    return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
}

GradientDrawable createPanelBg(Context ctx) {
    GradientDrawable gd = new GradientDrawable();
    gd.setColor(BG_PANEL);
    gd.setCornerRadius(dp(20));
    return gd;
}

GradientDrawable createCardBg(Context ctx) {
    GradientDrawable gd = new GradientDrawable();
    gd.setColor(BG_CARD);
    gd.setCornerRadius(dp(12));
    return gd;
}

GradientDrawable createInputBg(Context ctx) {
    GradientDrawable gd = new GradientDrawable();
    gd.setColor(BG_INPUT);
    gd.setCornerRadius(dp(8));
    return gd;
}

TextView createChip(Context ctx, String text, boolean active) {
    TextView tv = new TextView(ctx);
    tv.setText(text);
    tv.setTextSize(11);
    tv.setTextColor(active ? TEXT_MAIN : TEXT_SUB);
    tv.setBackground(createChipBg(ctx, active));
    tv.setPadding(dp(14), dp(6), dp(14), dp(6));
    return tv;
}

GradientDrawable createChipBg(Context ctx, boolean active) {
    GradientDrawable gd = new GradientDrawable();
    if (active) {
        gd.setColor(Color.parseColor("#1A3A2A"));
        gd.setStroke(dp(1), ACCENT_GREEN);
    } else {
        gd.setColor(BG_INPUT);
    }
    gd.setCornerRadius(dp(99));
    return gd;
}

GradientDrawable createSelectableChipBg(Context ctx, boolean selected, String mode) {
    GradientDrawable gd = new GradientDrawable();
    if (selected) {
        int color = getModeColor(mode);
        gd.setColor(Color.parseColor("#1A2A3A"));
        gd.setStroke(dp(1), color);
    } else {
        gd.setColor(BG_INPUT);
        gd.setStroke(dp(1), DIVIDER);
    }
    gd.setCornerRadius(dp(8));
    return gd;
}

GradientDrawable createModeChipBg(Context ctx, String mode) {
    GradientDrawable gd = new GradientDrawable();
    gd.setColor(BG_INPUT);
    gd.setCornerRadius(dp(6));
    gd.setStroke(dp(1), getModeColor(mode));
    return gd;
}

int getModeColor(String mode) {
    if ("keyword".equals(mode)) return ACCENT_BLUE;
    if ("regex".equals(mode)) return ACCENT_GOLD;
    if ("at".equals(mode)) return ACCENT_GREEN;
    return TEXT_SUB;
}

String getModeLabel(String mode) {
    if ("keyword".equals(mode)) return "关键词";
    if ("regex".equals(mode)) return "正则";
    if ("at".equals(mode)) return "艾特";
    return mode;
}
