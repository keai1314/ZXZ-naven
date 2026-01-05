package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.Version;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender2D;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventShader;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleManager;
import com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.SmoothAnimationTimer;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.text.CustomTextRenderer;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import java.awt.Color;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.AbstractMap;
import java.util.stream.Stream;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.commons.lang3.StringUtils;
import org.joml.Vector4f;
import com.heypixel.heypixelmod.obsoverlay.ui.LanguageManager;

@ModuleInfo(
        name = "NewHUD",
        description = "在屏幕上显示信息",
        category = Category.RENDER
)
public class NewHUD extends Module {
    // 更新颜色方案 - 现代透明风格
    public static final int headerColor = new Color(240, 240, 240, 100).getRGB(); // 淡白色半透明标题栏
    public static final int bodyColor = new Color(255, 255, 255, 30).getRGB(); // 非常透明的背景
    public static final int backgroundColor = new Color(255, 255, 255, 10).getRGB(); // 极低透明度背景
    public static final int accentColor = new Color(255, 255, 255, 255).getRGB(); // 纯白色调强调色
    public static final int secondaryAccent = new Color(220, 220, 220, 200).getRGB(); // 半透明白色辅助色
    public static final int infoColor = new Color(200, 200, 200, 150).getRGB(); // 信息灰色
    private static final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("M月d日");

    // 功能列表设置
    public BooleanValue arrayList = ValueBuilder.create(this, "功能列表").setDefaultBooleanValue(true).build().getBooleanValue();
    public BooleanValue prettyModuleName = ValueBuilder.create(this, "漂亮的模块名称")
            .setOnUpdate(value -> Module.update = true)
            .setVisibility(this.arrayList::getCurrentValue)
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();
    public BooleanValue hideRenderModules = ValueBuilder.create(this, "隐藏渲染模块")
            .setOnUpdate(value -> Module.update = true)
            .setVisibility(this.arrayList::getCurrentValue)
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();
    public BooleanValue rainbow = ValueBuilder.create(this, "彩虹")
            .setDefaultBooleanValue(true)
            .setVisibility(this.arrayList::getCurrentValue)
            .build()
            .getBooleanValue();
    public FloatValue rainbowSpeed = ValueBuilder.create(this, "彩虹变色速度")
            .setVisibility(this.arrayList::getCurrentValue)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(20.0F)
            .setDefaultFloatValue(8.0F)
            .setFloatStep(0.1F)
            .build()
            .getFloatValue();
    public FloatValue rainbowOffset = ValueBuilder.create(this, "彩虹偏移")
            .setVisibility(this.arrayList::getCurrentValue)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(20.0F)
            .setDefaultFloatValue(8.0F)
            .setFloatStep(0.1F)
            .build()
            .getFloatValue();
    public ModeValue arrayListDirection = ValueBuilder.create(this, "功能列表方向")
            .setVisibility(this.arrayList::getCurrentValue)
            .setDefaultModeIndex(1) // 默认向左，与参考图一致
            .setModes("向右", "向左")
            .build()
            .getModeValue();
    public FloatValue xOffset = ValueBuilder.create(this, "X偏移")
            .setVisibility(this.arrayList::getCurrentValue)
            .setMinFloatValue(-100.0F)
            .setMaxFloatValue(100.0F)
            .setDefaultFloatValue(5.0F)
            .setFloatStep(1.0F)
            .build()
            .getFloatValue();
    public FloatValue yOffset = ValueBuilder.create(this, "Y偏移")
            .setVisibility(this.arrayList::getCurrentValue)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(100.0F)
            .setDefaultFloatValue(30.0F) // 为顶部信息留出空间
            .setFloatStep(1.0F)
            .build()
            .getFloatValue();
    public FloatValue arrayListSize = ValueBuilder.create(this, "功能表列大小")
            .setVisibility(this.arrayList::getCurrentValue)
            .setDefaultFloatValue(0.5F)
            .setFloatStep(0.01F)
            .setMinFloatValue(0.1F)
            .setMaxFloatValue(1.0F)
            .build()
            .getFloatValue();
    public FloatValue moduleSpacing = ValueBuilder.create(this, "功能间距")
            .setVisibility(this.arrayList::getCurrentValue)
            .setDefaultFloatValue(1.2F)
            .setFloatStep(0.05F)
            .setMinFloatValue(0.5F)
            .setMaxFloatValue(2.0F)
            .build()
            .getFloatValue();

    // 左侧信息面板设置
    public BooleanValue leftInfoPanel = ValueBuilder.create(this, "左侧信息面板").setDefaultBooleanValue(true).build().getBooleanValue();
    public FloatValue leftPanelSize = ValueBuilder.create(this, "左侧面板大小")
            .setVisibility(this.leftInfoPanel::getCurrentValue)
            .setDefaultFloatValue(0.45F)
            .setFloatStep(0.01F)
            .setMinFloatValue(0.1F)
            .setMaxFloatValue(1.0F)
            .build()
            .getFloatValue();
    public FloatValue leftPanelXOffset = ValueBuilder.create(this, "左侧面板X偏移")
            .setVisibility(this.leftInfoPanel::getCurrentValue)
            .setMinFloatValue(-500.0F)
            .setMaxFloatValue(500.0F)
            .setDefaultFloatValue(5.0F)
            .setFloatStep(1.0F)
            .build()
            .getFloatValue();
    public FloatValue leftPanelYOffset = ValueBuilder.create(this, "左侧面板Y偏移")
            .setVisibility(this.leftInfoPanel::getCurrentValue)
            .setMinFloatValue(-500.0F)
            .setMaxFloatValue(500.0F)
            .setDefaultFloatValue(5.0F)
            .setFloatStep(1.0F)
            .build()
            .getFloatValue();
    // 修改透明度设置，使其与Inventory.java中的一致
    public FloatValue leftPanelAlpha = ValueBuilder.create(this, "面板透明度")
            .setVisibility(this.leftInfoPanel::getCurrentValue)
            .setDefaultFloatValue(0.8F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(1.0F)
            .setFloatStep(0.05F)
            .build()
            .getFloatValue();
    public BooleanValue showLeftPanelBackground = ValueBuilder.create(this, "显示面板背景")
            .setVisibility(this.leftInfoPanel::getCurrentValue)
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();
    // 添加左侧面板圆滑度设置
    public FloatValue leftPanelRoundness = ValueBuilder.create(this, "面板圆滑度")
            .setVisibility(this.leftInfoPanel::getCurrentValue)
            .setDefaultFloatValue(0.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(15.0F)
            .setFloatStep(0.5F)
            .build()
            .getFloatValue();

    // 其他设置
    public BooleanValue moduleToggleSound = ValueBuilder.create(this, "模块切换声音").setDefaultBooleanValue(true).build().getBooleanValue();
    public BooleanValue notification = ValueBuilder.create(this, "通知").setDefaultBooleanValue(true).build().getBooleanValue();
    
    // 水印设置
    public BooleanValue rainbowFirstLetter = ValueBuilder.create(this, "首字母变色").setDefaultBooleanValue(false).build().getBooleanValue();
    public FloatValue rainbowSpeedWatermark = ValueBuilder.create(this, "水印变色速度")
            .setVisibility(this.rainbowFirstLetter::getCurrentValue)
            .setDefaultFloatValue(1.0F)
            .setMinFloatValue(0.1F)
            .setMaxFloatValue(5.0F)
            .setFloatStep(0.1F)
            .build()
            .getFloatValue();

    // 添加"更好的功能开关提示"设置
    public BooleanValue betterModuleToggleNotification = ValueBuilder.create(this, "更好的功能开关提示").setDefaultBooleanValue(true).build().getBooleanValue();
    public FloatValue toggleNotificationY = ValueBuilder.create(this, "开关提示Y坐标")
            .setVisibility(this.betterModuleToggleNotification::getCurrentValue)
            .setDefaultFloatValue(50.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(1000.0F)
            .setFloatStep(1.0F)
            .build()
            .getFloatValue();
    public FloatValue toggleNotificationAlpha = ValueBuilder.create(this, "开关提示透明度")
            .setVisibility(this.betterModuleToggleNotification::getCurrentValue)
            .setDefaultFloatValue(0.8F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(1.0F)
            .setFloatStep(0.05F)
            .build()
            .getFloatValue();
    public BooleanValue showToggleNotificationBackground = ValueBuilder.create(this, "显示开关提示背景")
            .setVisibility(this.betterModuleToggleNotification::getCurrentValue)
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();
    // 添加开关提示圆滑度设置
    public FloatValue toggleNotificationRoundness = ValueBuilder.create(this, "开关提示圆滑度")
            .setVisibility(this.betterModuleToggleNotification::getCurrentValue)
            .setDefaultFloatValue(0.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(15.0F)
            .setFloatStep(0.5F)
            .build()
            .getFloatValue();

    // 添加ClickGUI背景透明度设置
    public FloatValue clickGUIBackgroundAlpha = ValueBuilder.create(this, "ClickGUI背景透明度")
            .setDefaultFloatValue(0.8F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(1.0F)
            .setFloatStep(0.05F)
            .build()
            .getFloatValue();

    // 添加按键显示设置
    public BooleanValue keyBindDisplay = ValueBuilder.create(this, "按键显示").setDefaultBooleanValue(true).build().getBooleanValue();
    public ModeValue keyBindMode = ValueBuilder.create(this, "按键显示模式")
            .setVisibility(this.keyBindDisplay::getCurrentValue)
            .setDefaultModeIndex(0)
            .setModes("Type 1")
            .build()
            .getModeValue();
    public FloatValue keyBindX = ValueBuilder.create(this, "按键显示X坐标")
            .setVisibility(this.keyBindDisplay::getCurrentValue)
            .setDefaultFloatValue(5.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(1000.0F)
            .setFloatStep(1.0F)
            .build()
            .getFloatValue();
    public FloatValue keyBindY = ValueBuilder.create(this, "按键显示Y坐标")
            .setVisibility(this.keyBindDisplay::getCurrentValue)
            .setDefaultFloatValue(200.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(1000.0F)
            .setFloatStep(1.0F)
            .build()
            .getFloatValue();
    public FloatValue keyBindScale = ValueBuilder.create(this, "按键显示大小")
            .setVisibility(this.keyBindDisplay::getCurrentValue)
            .setDefaultFloatValue(0.5F)
            .setMinFloatValue(0.1F)
            .setMaxFloatValue(2.0F)
            .setFloatStep(0.05F)
            .build()
            .getFloatValue();
    public FloatValue keyBindAlpha = ValueBuilder.create(this, "按键显示透明度")
            .setVisibility(this.keyBindDisplay::getCurrentValue)
            .setDefaultFloatValue(0.8F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(1.0F)
            .setFloatStep(0.05F)
            .build()
            .getFloatValue();
    public BooleanValue showKeyBindBackground = ValueBuilder.create(this, "显示按键背景")
            .setVisibility(this.keyBindDisplay::getCurrentValue)
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();
    public FloatValue keyBindRoundness = ValueBuilder.create(this, "按键显示圆滑度")
            .setVisibility(this.keyBindDisplay::getCurrentValue)
            .setDefaultFloatValue(0.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(15.0F)
            .setFloatStep(0.5F)
            .build()
            .getFloatValue();

    // 用于存储模块开关提示的列表
    private final List<ModuleToggleNotification> moduleToggleNotifications = new CopyOnWriteArrayList<>();

    // 模块开关通知类
// 修改 ModuleToggleNotification 类以支持多行显示
    public static class ModuleToggleNotification {
        private final String message;
        private final List<String> lines;
        private final long createTime;
        private final long maxAge = 3000; // 3秒显示时间

        public ModuleToggleNotification(String message, Module module) {
            // 使用本地化消息
            String localizedMessage = message;
            try {
                if (module != null) {
                    java.lang.reflect.Method method = module.getClass().getMethod("getLocalizedName");
                    if (method != null) {
                        String localizedName = (String) method.invoke(module);
                        // 替换消息中的模块名称为本地化名称
                        if (LanguageManager.getInstance().isChinese()) {
                            localizedMessage = message.replace(module.getName(), localizedName);
                        } else {
                            // 英文模式下，将"XXX 开启了!"转换为"XXX Enabled!"或"XXX 关闭了!"转换为"XXX Disabled!"
                            if (message.contains("开启了")) {
                                localizedMessage = localizedName + " Enabled!";
                            } else if (message.contains("关闭了")) {
                                localizedMessage = localizedName + " Disabled!";
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // 本地化失败，使用原始消息
            }
            
            this.message = localizedMessage;
            // 按逗号分割消息为多行，使用List.of()工厂方法
            this.lines = List.of(localizedMessage.split(", "));
            this.createTime = System.currentTimeMillis();
        }

        public String getMessage() {
            return message;
        }

        public List<String> getLines() {
            return lines;
        }

        public long getCreateTime() {
            return createTime;
        }

        public long getMaxAge() {
            return maxAge;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - createTime > maxAge;
        }
    }

    List<Module> renderModules;
    float width;
    float watermarkHeight;
    List<Vector4f> blurMatrices = new ArrayList<>(); // 保留ArrayList，因为需要频繁修改

    public String getModuleDisplayName(Module module) {
        // 使用本地化方法获取模块名称
        String name = this.prettyModuleName.getCurrentValue() ? getLocalizedName(module) : getLocalizedName(module);
        return name + (module.getSuffix() == null ? "" : " §7" + module.getSuffix());
    }

    @EventTarget
    public void notification(EventRender2D e) {
        if (this.notification.getCurrentValue()) {
            Naven.getInstance().getNotificationManager().onRender(e);
        }
    }

    @EventTarget
    public void onShader(EventShader e) {
        if (this.notification.getCurrentValue() && e.getType() == EventType.SHADOW) {
            Naven.getInstance().getNotificationManager().onRenderShadow(e);
        }

        if (this.arrayList.getCurrentValue()) {
            for (Vector4f blurMatrix : this.blurMatrices) {
                RenderUtils.fillBound(e.getStack(), blurMatrix.x(), blurMatrix.y(), blurMatrix.z(), blurMatrix.w(), 1073741824);
            }
        }

        // 左侧信息面板阴影
        if (this.leftInfoPanel.getCurrentValue()) {
            int panelWidth = 150; // 扩宽面板以适应大坐标值
            int panelHeight = 140;
            RenderUtils.drawRoundedRect(e.getStack(), 5 + this.leftPanelXOffset.getCurrentValue(), 5 + this.leftPanelYOffset.getCurrentValue(), panelWidth, panelHeight, 3.0F, Integer.MIN_VALUE);
        }
    }

    @EventTarget
    public void onRender(EventRender2D e) {
        // 每次渲染时检查语言设置是否变化
        checkLanguageUpdate();
        
        CustomTextRenderer font = Fonts.opensans;
        Minecraft mc = Minecraft.getInstance();

        // 水印
        e.getStack().pushPose();
        String clientName = "Naven";
        float navanX = 5.0F;
        float navanY = 5.0F;
        
        if (this.rainbowFirstLetter.getCurrentValue()) {
            // 首字母变色
            String firstLetter = clientName.substring(0, 1);
            String remainingLetters = clientName.substring(1);
            
            // 获取彩虹颜色
            int rainbowColor = RenderUtils.getRainbowOpaque(
                    0, 1.0F, 1.0F, (21.0F - this.rainbowSpeedWatermark.getCurrentValue()) * 1000.0F
            );
            
            // 渲染变色的首字母
            font.render(e.getStack(), firstLetter, navanX, navanY, new Color(rainbowColor), true, 0.6);
            
            // 渲染剩余字母
            float firstLetterWidth = font.getWidth(firstLetter, 0.6);
            font.render(e.getStack(), remainingLetters, navanX + firstLetterWidth, navanY, new Color(255, 255, 255, 200), true, 0.6);
        } else {
            // 默认渲染
            font.render(e.getStack(), clientName, navanX, navanY, new Color(255, 255, 255, 200), true, 0.6);
        }
        
        e.getStack().popPose();

        // 绘制左侧信息面板
        if (this.leftInfoPanel.getCurrentValue() && mc.player != null) {
            e.getStack().pushPose();
            int panelWidth = 150; // 扩宽面板以适应大坐标值
            int panelHeight = 140;
            float startX = 5 + this.leftPanelXOffset.getCurrentValue();
            float startY = 5 + this.leftPanelYOffset.getCurrentValue();

            if (showLeftPanelBackground.getCurrentValue()) {
                float bgAlpha = leftPanelAlpha.getCurrentValue();
                int bgColor = new Color(255, 255, 255, (int) (255 * bgAlpha)).getRGB();
                float roundnessValue = leftPanelRoundness.getCurrentValue();
                if (roundnessValue > 0) {
                    RenderUtils.drawRoundedRect(e.getStack(), startX, startY, panelWidth, panelHeight, roundnessValue, bgColor);
                } else {
                    RenderUtils.fillBound(e.getStack(), startX, startY, panelWidth, panelHeight, bgColor);
                }
            }

            // 绘制标题栏 - 淡白色
            float roundnessValue = leftPanelRoundness.getCurrentValue();
            int panelHeaderColor = new Color(240, 240, 240, 100).getRGB(); // 固定标题栏透明度
            if (roundnessValue > 0) {
                // 只对顶部进行圆角处理
                RenderUtils.drawRoundedRect(e.getStack(), startX, startY, panelWidth, 15, roundnessValue, panelHeaderColor);
            } else {
                RenderUtils.fill(e.getStack(), startX, startY, startX + panelWidth, startY + 15, panelHeaderColor);
            }
            font.render(e.getStack(), "我的手艺", (double)(startX + 5), (double)(startY + 2), Color.BLACK, true, (double)this.leftPanelSize.getCurrentValue() + 0.1);

            // 绘制玩家信息
            LocalPlayer player = mc.player;
            float yPos = startY + 20;
            float lineHeight = (float)(font.getHeight(true, (double)this.leftPanelSize.getCurrentValue()) * 1.2);

            // 显示实时时间
            String currentTime = timeFormat.format(new Date());
            drawLeftPanelText(font, e, "时间", currentTime, startX, yPos, lineHeight, this.leftPanelSize.getCurrentValue());
            yPos += lineHeight;

            // 显示当前日期
            String currentDate = dateFormat.format(new Date());
            drawLeftPanelText(font, e, "日期", currentDate, startX, yPos, lineHeight, this.leftPanelSize.getCurrentValue());
            yPos += lineHeight;

            // 显示玩家坐标
            String playerCoords = String.format("%.1f %.1f %.1f", player.getX(), player.getY(), player.getZ());
            drawLeftPanelText(font, e, "当前坐标", playerCoords, startX, yPos, lineHeight, this.leftPanelSize.getCurrentValue());
            yPos += lineHeight;

            // 保留第四个和第五个信息供手动填写
            drawLeftPanelText(font, e, "Dev", "Lingma", startX, yPos, lineHeight, this.leftPanelSize.getCurrentValue());
            yPos += lineHeight;

            drawLeftPanelText(font, e, "IQ", "101", startX, yPos, lineHeight, this.leftPanelSize.getCurrentValue());

            e.getStack().popPose();
        }

        // 绘制功能列表 - 左侧垂直排列
        this.blurMatrices.clear();
        if (this.arrayList.getCurrentValue()) {
            e.getStack().pushPose();
            ModuleManager moduleManager = Naven.getInstance().getModuleManager();
            if (Module.update || this.renderModules == null) {
                this.renderModules = new ArrayList<>(moduleManager.getModules());
                if (this.hideRenderModules.getCurrentValue()) {
                    this.renderModules.removeIf(modulex -> modulex.getCategory() == Category.RENDER);
                }

                // 按模块名称长度排序
                this.renderModules.sort((o1, o2) -> {
                    float o1Width = font.getWidth(this.getModuleDisplayName(o1), (double)this.arrayListSize.getCurrentValue());
                    float o2Width = font.getWidth(this.getModuleDisplayName(o2), (double)this.arrayListSize.getCurrentValue());
                    return Float.compare(o2Width, o1Width);
                });
                Module.update = false;
            }

            float maxWidth = this.renderModules.isEmpty()
                    ? 0.0F
                    : font.getWidth(this.getModuleDisplayName(this.renderModules.get(0)), (double)this.arrayListSize.getCurrentValue());
            float arrayListX = this.arrayListDirection.isCurrentMode("向右")
                    ? (float)mc.getWindow().getGuiScaledWidth() - maxWidth - 6.0F + this.xOffset.getCurrentValue()
                    : 3.0F + this.xOffset.getCurrentValue();
            float arrayListY = this.yOffset.getCurrentValue();
            float height = 0.0F;
            double fontHeight = font.getHeight(true, (double)this.arrayListSize.getCurrentValue()) * this.moduleSpacing.getCurrentValue(); // 使用自定义间距

            for (Module module : this.renderModules) {
                SmoothAnimationTimer animation = module.getAnimation();
                if (module.isEnabled()) {
                    animation.target = 100.0F;
                } else {
                    animation.target = 0.0F;
                }

                animation.update(true);
                if (animation.value > 0.0F) {
                    String displayName = this.getModuleDisplayName(module);
                    float stringWidth = font.getWidth(displayName, (double)this.arrayListSize.getCurrentValue());
                    float left = -stringWidth * (1.0F - animation.value / 100.0F);
                    float right = maxWidth - stringWidth * (animation.value / 100.0F);
                    float innerX = this.arrayListDirection.isCurrentMode("向左") ? left : right;

                    this.blurMatrices.add(
                            new Vector4f(arrayListX + innerX, arrayListY + height, stringWidth, (float)fontHeight)
                    );

                    // 文字颜色 - 彩虹效果或白色
                    int color = Color.WHITE.getRGB();
                    if (this.rainbow.getCurrentValue()) {
                        color = RenderUtils.getRainbowOpaque(
                                (int)(-height * this.rainbowOffset.getCurrentValue()), 1.0F, 1.0F, (21.0F - this.rainbowSpeed.getCurrentValue()) * 1000.0F
                        );
                    }

                    float alpha = animation.value / 100.0F;
                    font.setAlpha(alpha);
                    font.render(
                            e.getStack(),
                            displayName,
                            (double)(arrayListX + innerX), // 移除额外的偏移量
                            (double)(arrayListY + height),
                            new Color(color),
                            true,
                            (double)this.arrayListSize.getCurrentValue()
                    );
                    height += (float)fontHeight; // 使用自定义行高
                }
            }

            font.setAlpha(1.0F);
            e.getStack().popPose();
        }

        // 绘制更好的模块开关提示
        if (this.betterModuleToggleNotification.getCurrentValue()) {
            renderModuleToggleNotifications(e);
        }

        // 绘制按键绑定显示
        if (this.keyBindDisplay.getCurrentValue() && mc.player != null && mc.options != null) {
            e.getStack().pushPose();

            float x = this.keyBindX.getCurrentValue();
            float y = this.keyBindY.getCurrentValue();
            float scale = this.keyBindScale.getCurrentValue();
            float roundnessValue = this.keyBindRoundness.getCurrentValue();

            // 获取当前按下的按键
            List<String> pressedKeys = getPressedKeys();

            if (!pressedKeys.isEmpty()) {
                CustomTextRenderer keyFont = Fonts.opensans;

                // 计算背景尺寸
                float maxWidth = 0;
                float totalHeight = 0;
                float padding = 4.0F;
                float lineHeight = (float) (keyFont.getHeight(true, scale) * 1.2);

                for (String key : pressedKeys) {
                    float width = keyFont.getWidth(key, scale);
                    if (width > maxWidth) {
                        maxWidth = width;
                    }
                    totalHeight += lineHeight;
                }

                float bgWidth = maxWidth + padding * 2;
                float bgHeight = totalHeight + padding;

                // 绘制背景
                if (showKeyBindBackground.getCurrentValue()) {
                    float bgAlpha = keyBindAlpha.getCurrentValue();
                    int bgColor = new Color(0, 0, 0, (int) (255 * bgAlpha)).getRGB();
                    if (roundnessValue > 0) {
                        RenderUtils.drawRoundedRect(e.getStack(), x, y, bgWidth, bgHeight, roundnessValue, bgColor);
                    } else {
                        RenderUtils.fillBound(e.getStack(), x, y, bgWidth, bgHeight, bgColor);
                    }
                }

                // 绘制按键文本
                float currentY = y + padding / 2;
                for (String key : pressedKeys) {
                    float textWidth = keyFont.getWidth(key, scale);
                    float textX = x + (bgWidth - textWidth) / 2;
                    keyFont.render(e.getStack(), key, textX, currentY, Color.WHITE, true, scale);
                    currentY += lineHeight;
                }
            }

            e.getStack().popPose();
        }
    }

    // 辅助方法：绘制模块开关通知
    private void renderModuleToggleNotifications(EventRender2D e) {
        CustomTextRenderer font = Fonts.opensans;
        Minecraft mc = Minecraft.getInstance();

        // 移除过期的通知
        moduleToggleNotifications.removeIf(ModuleToggleNotification::isExpired);

        // 如果没有通知，直接返回
        if (moduleToggleNotifications.isEmpty()) {
            return;
        }

        // 获取最新的通知
        ModuleToggleNotification notification = moduleToggleNotifications.get(0);

        // 计算通知的生命周期和透明度
        long lifeTime = System.currentTimeMillis() - notification.getCreateTime();
        float alphaFactor = Math.min(1.0f, lifeTime / 500.0f); // 淡入效果
        alphaFactor = Math.min(alphaFactor, (notification.getMaxAge() - lifeTime) / 500.0f); // 淡出效果

        List<String> lines = notification.getLines();
        double scale = 0.6;
        float textHeight = (float) font.getHeight(true, scale);
        float lineHeight = textHeight * 1.2f; // 行间距

        // 计算整体尺寸和位置
        float padding = 12.0F;
        float totalHeight = lines.size() * lineHeight + padding * 2;
        
        // 计算最大文本宽度
        float maxTextWidth = 0;
        for (String line : lines) {
            float lineWidth = font.getWidth(line, scale);
            if (lineWidth > maxTextWidth) {
                maxTextWidth = lineWidth;
            }
        }
        float totalWidth = maxTextWidth + padding * 2;
        
        // 计算居中位置
        float centerX = mc.getWindow().getGuiScaledWidth() / 2.0f;
        float centerY = toggleNotificationY.getCurrentValue();
        float bgX = centerX - totalWidth / 2;
        float bgY = centerY - totalHeight / 2;
        
        // 为毛玻璃效果添加到blurMatrices
        this.blurMatrices.add(new Vector4f(bgX, bgY, totalWidth, totalHeight));
        
        // 绘制彩虹外发光
        float glowSize = 2.0F;
        float glowAlpha = 0.4F * alphaFactor;
        for (int i = 0; i < 4; i++) {
            float glowOffset = glowSize * i;
            int rainbowColor = RenderUtils.getRainbowOpaque(
                    0, 1.0F, 1.0F, (21.0F - this.rainbowSpeed.getCurrentValue()) * 1000.0F
            );
            int glowColor = new Color(
                    (rainbowColor >> 16) & 0xFF,
                    (rainbowColor >> 8) & 0xFF,
                    rainbowColor & 0xFF,
                    (int) (255 * glowAlpha / (i + 1))
            ).getRGB();
            RenderUtils.drawRoundedRect(e.getStack(), 
                    bgX - glowOffset, bgY - glowOffset, 
                    totalWidth + glowOffset * 2, totalHeight + glowOffset * 2, 
                    15.0F, glowColor);
        }
        
        // 绘制半透明圆角矩形背景
        if (showToggleNotificationBackground.getCurrentValue()) {
            float bgAlpha = toggleNotificationAlpha.getCurrentValue() * alphaFactor * 0.3f; // 更透明的背景
            int bgColor = new Color(255, 255, 255, (int) (255 * bgAlpha)).getRGB();
            RenderUtils.drawRoundedRect(e.getStack(), bgX, bgY, totalWidth, totalHeight, 15.0F, bgColor);
        }
        
        // 绘制文本
        float textY = centerY - (lines.size() * lineHeight) / 2;
        for (String line : lines) {
            float textWidth = font.getWidth(line, scale);
            float textX = centerX - textWidth / 2;
            int textColor = new Color(255, 255, 255, (int) (255 * alphaFactor)).getRGB();
            font.render(e.getStack(), line, textX, textY, new Color(textColor), true, scale);
            textY += lineHeight;
        }
    }

    // 辅助方法：绘制左侧面板的文本
    private void drawLeftPanelText(CustomTextRenderer font, EventRender2D e, String label, String value, float startX, float yPos, float lineHeight, float textSize) {
        // 绘制标签（灰色）- 移动到面板最左侧
        font.render(e.getStack(), label, (double)(startX + 5), (double)yPos, Color.BLACK, true, (double)textSize);
        // 绘制值（亮色）- 保持原有位置
        float valueX = startX + 145 - font.getWidth(value, (double)textSize);
        font.render(e.getStack(), value, (double)valueX, (double)yPos, Color.BLACK, true, (double)textSize);
    }

    // 添加方法用于添加模块开关通知
    public void addModuleToggleNotification(String message) {
        if (this.betterModuleToggleNotification.getCurrentValue()) {
            // 移除过期的通知
            moduleToggleNotifications.removeIf(ModuleToggleNotification::isExpired);

            // 添加新通知到列表开头
            moduleToggleNotifications.add(0, new ModuleToggleNotification(message, null));

            // 保持最多只显示一个通知
            while (moduleToggleNotifications.size() > 1) {
                moduleToggleNotifications.remove(1);
            }
        }
    }

    // 添加带模块参数的方法用于添加模块开关通知
    public void addModuleToggleNotification(String message, Module module) {
        if (this.betterModuleToggleNotification.getCurrentValue()) {
            // 移除过期的通知
            moduleToggleNotifications.removeIf(ModuleToggleNotification::isExpired);

            // 添加新通知到列表开头
            moduleToggleNotifications.add(0, new ModuleToggleNotification(message, module));

            // 保持最多只显示一个通知
            while (moduleToggleNotifications.size() > 1) {
                moduleToggleNotifications.remove(1);
            }
        }
    }

    // 添加获取本地化模块名称的方法
    private String getLocalizedName(Module module) {
        // 使用反射调用模块的getLocalizedName方法（如果存在）
        try {
            java.lang.reflect.Method method = module.getClass().getMethod("getLocalizedName");
            if (method != null) {
                return (String) method.invoke(module);
            }
        } catch (Exception e) {
            // 方法不存在或调用失败，使用默认名称
        }
        
        // 默认返回模块名称
        return module.getName();
    }

    // 添加获取本地化模块描述的方法
    private String getLocalizedDescription(Module module) {
        // 使用反射调用模块的getLocalizedDescription方法（如果存在）
        try {
            java.lang.reflect.Method method = module.getClass().getMethod("getLocalizedDescription");
            if (method != null) {
                return (String) method.invoke(module);
            }
        } catch (Exception e) {
            // 方法不存在或调用失败，使用默认描述
        }
        
        // 默认返回模块描述
        return module.getDescription();
    }

    // 添加获取当前按下的按键的方法
    private List<String> getPressedKeys() {
        List<String> pressedKeys = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();

        if (mc.options == null || mc.getWindow() == null) {
            return pressedKeys;
        }

        // 检查常用按键
        if (isKeyDown(mc.options.keyUp)) pressedKeys.add("W");
        if (isKeyDown(mc.options.keyLeft)) pressedKeys.add("A");
        if (isKeyDown(mc.options.keyDown)) pressedKeys.add("S");
        if (isKeyDown(mc.options.keyRight)) pressedKeys.add("D");
        if (isKeyDown(mc.options.keyJump)) pressedKeys.add("Space");
        if (isKeyDown(mc.options.keyShift)) pressedKeys.add("Shift");
        if (isKeyDown(mc.options.keySprint)) pressedKeys.add("Ctrl");
        if (isKeyDown(mc.options.keyAttack)) pressedKeys.add("LMB");
        if (isKeyDown(mc.options.keyUse)) pressedKeys.add("RMB");

        return pressedKeys;
    }

    // 辅助方法检查按键是否被按下
    private boolean isKeyDown(com.mojang.blaze3d.platform.InputConstants.Key key) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getWindow() == null) return false;
        return com.mojang.blaze3d.platform.InputConstants.isKeyDown(
                mc.getWindow().getWindow(), key.getValue()
        );
    }

    // 重载方法以兼容不同版本的MC
    private boolean isKeyDown(net.minecraft.client.KeyMapping keyMapping) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getWindow() == null) return false;
        return com.mojang.blaze3d.platform.InputConstants.isKeyDown(
                mc.getWindow().getWindow(), keyMapping.getKey().getValue()
        );
    }

    // 添加检查语言更新的方法
    private void checkLanguageUpdate() {
        try {
            // 获取ClickGUIModule中的语言设置
            ClickGUIModule clickGUIModule = (ClickGUIModule) Naven.getInstance().getModuleManager().getModule(ClickGUIModule.class);
            boolean currentChineseValue = clickGUIModule.chinese.getCurrentValue();
            
            // 如果语言设置发生变化，更新LanguageManager并强制刷新
            if (LanguageManager.getInstance().isChinese() != currentChineseValue) {
                LanguageManager.getInstance().setLanguage(currentChineseValue);
                Module.update = true;
            }
        } catch (Exception ex) {
            // 忽略异常
        }
    }

    public String getLocalizedName() {
      return LanguageManager.getInstance().getLocalizedString("NewHUD", "NewHUD");
   }

   public String getLocalizedDescription() {
      return LanguageManager.getInstance().getLocalizedString("在屏幕上显示信息", "Display information on the screen");
   }
}
