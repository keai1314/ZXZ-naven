package com.heypixel.heypixelmod.obsoverlay.ui.components;

import com.heypixel.heypixelmod.obsoverlay.modules.impl.misc.MusicPlayer;
import com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.Colors;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.text.CustomTextRenderer;
import com.heypixel.heypixelmod.obsoverlay.values.impl.StringValue;
import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.Color;
import java.io.File;
import java.util.List;

public class MusicPlayerComponent {
    private final MusicPlayer musicPlayer;
    private boolean hoveringAddButton = false;
    private boolean hoveringPlayButton = false;
    private boolean hoveringNextButton = false;
    private boolean hoveringClearButton = false;
    private boolean hoveringDeleteButton = false;
    private boolean hoveringDeleteConfirmButton = false;
    private boolean hoveringInputField = false;
    private boolean hoveringDeleteInputField = false;
    
    // 添加防重复点击机制
    private long lastAddClickTime = 0;
    private long lastPlayClickTime = 0;
    private long lastNextClickTime = 0;
    private long lastClearClickTime = 0;
    private long lastDeleteClickTime = 0;
    private long lastDeleteConfirmClickTime = 0;
    private static final long CLICK_DELAY = 200; // 200毫秒的点击延迟
    
    public MusicPlayerComponent(MusicPlayer musicPlayer) {
        this.musicPlayer = musicPlayer;
    }
    
    public void render(PoseStack stack, float x, float y, int mouseX, int mouseY, float motion, StringValue editingValue, String editingText, int cursorPosition, boolean cursorVisible) {
        CustomTextRenderer opensans = Fonts.opensans;
        
        // 计算起始Y位置
        float startY = y + motion;
        
        // 渲染音乐路径输入框标签
        opensans.render(stack, "音乐路径:", (double)x, (double)startY, Color.WHITE, true, 0.4);
        
        // 渲染快捷键提示
        opensans.render(stack, "(Ctrl+V粘贴)", (double)(x + 150), (double)startY, Color.GRAY, true, 0.3);
        
        // 渲染音乐路径输入框
        String pathValue = musicPlayer.getMusicPathValue().getCurrentValue();
        if (pathValue == null) pathValue = "";
        
        // 如果正在编辑此字段，则显示编辑中的文本
        boolean isEditing = (editingValue == musicPlayer.getMusicPathValue());
        String displayText = isEditing ? editingText : pathValue;
        
        hoveringInputField = RenderUtils.isHovering(
            mouseX, mouseY, 
            (int)x, (int)(startY + 15), 
            (int)(x + 200), (int)(startY + 35)
        );
        
        int inputFieldColor = hoveringInputField ? Colors.getColor(40, 40, 40, 200) : Colors.getColor(30, 30, 30, 200);
        RenderUtils.drawRoundedRect(stack, x, startY + 15, 200, 20, 3, inputFieldColor);
        
        // 渲染文本
        if (!displayText.isEmpty()) {
            opensans.render(stack, displayText, (double)(x + 5), (double)(startY + 20), Color.LIGHT_GRAY, true, 0.35);
        }
        
        // 渲染光标（如果正在编辑且光标可见）
        if (isEditing && cursorVisible) {
            // 计算光标位置
            float cursorX = x + 5 + opensans.getWidth(displayText.substring(0, Math.min(cursorPosition, displayText.length())), 0.35);
            // 绘制光标
            RenderUtils.fill(stack, cursorX, startY + 18, cursorX + 1, startY + 32, Color.WHITE.getRGB());
        }
        
        // 渲染添加按钮
        hoveringAddButton = RenderUtils.isHovering(
            mouseX, mouseY, 
            (int)(x + 210), (int)(startY + 15), 
            (int)(x + 270), (int)(startY + 35)
        );
        
        int buttonColor = hoveringAddButton ? Colors.getColor(70, 70, 70, 200) : Colors.getColor(50, 50, 50, 200);
        RenderUtils.drawRoundedRect(stack, x + 210, startY + 15, 60, 20, 3, buttonColor);
        opensans.render(stack, "添加至列表", (double)(x + 215), (double)(startY + 20), Color.WHITE, true, 0.35);
        
        // 渲染播放控制按钮
        float buttonY = startY + 45;
        
        // 渲染播放/暂停按钮
        hoveringPlayButton = RenderUtils.isHovering(
            mouseX, mouseY, 
            (int)(x), (int)(buttonY), 
            (int)(x + 60), (int)(buttonY + 20)
        );
        
        int playButtonColor = hoveringPlayButton ? Colors.getColor(70, 70, 70, 200) : Colors.getColor(50, 50, 50, 200);
        RenderUtils.drawRoundedRect(stack, x, buttonY, 60, 20, 3, playButtonColor);
        String playButtonText = musicPlayer.isPlaying() ? "停止" : "播放";
        opensans.render(stack, playButtonText, (double)(x + 5), (double)(buttonY + 5), Color.WHITE, true, 0.35);
        
        // 渲染下一首按钮
        hoveringNextButton = RenderUtils.isHovering(
            mouseX, mouseY, 
            (int)(x + 70), (int)(buttonY), 
            (int)(x + 130), (int)(buttonY + 20)
        );
        
        int nextButtonColor = hoveringNextButton ? Colors.getColor(70, 70, 70, 200) : Colors.getColor(50, 50, 50, 200);
        RenderUtils.drawRoundedRect(stack, x + 70, buttonY, 60, 20, 3, nextButtonColor);
        opensans.render(stack, "下一首", (double)(x + 75), (double)(buttonY + 5), Color.WHITE, true, 0.35);
        
        // 渲染清除播放列表按钮
        hoveringClearButton = RenderUtils.isHovering(
            mouseX, mouseY, 
            (int)(x + 140), (int)(buttonY), 
            (int)(x + 230), (int)(buttonY + 20)
        );
        
        int clearButtonColor = hoveringClearButton ? Colors.getColor(70, 70, 70, 200) : Colors.getColor(50, 50, 50, 200);
        RenderUtils.drawRoundedRect(stack, x + 140, buttonY, 90, 20, 3, clearButtonColor);
        opensans.render(stack, "清除播放列表", (double)(x + 145), (double)(buttonY + 5), Color.WHITE, true, 0.35);
        
        // 渲染状态消息
        String statusMessage = musicPlayer.getStatusMessageValue().getCurrentValue();
        if (statusMessage != null && !statusMessage.isEmpty()) {
            // 根据消息内容设置颜色（错误为红色，成功为绿色）
            Color statusColor = statusMessage.contains("错误") || statusMessage.contains("不存在") || statusMessage.contains("失败") ? 
                Color.RED : Color.GREEN;
            opensans.render(stack, statusMessage, (double)x, (double)(buttonY + 25), statusColor, true, 0.35);
        }
        
        // 渲染删除歌曲部分
        float deleteY = buttonY + 45;
        opensans.render(stack, "删除歌曲:", (double)x, (double)deleteY, Color.WHITE, true, 0.4);
        
        // 渲染删除歌曲索引输入框
        String deleteIndexValue = musicPlayer.getDeleteSongIndexValue().getCurrentValue();
        if (deleteIndexValue == null) deleteIndexValue = "";
        
        // 如果正在编辑删除索引字段，则显示编辑中的文本
        boolean isEditingDelete = (editingValue == musicPlayer.getDeleteSongIndexValue());
        String deleteDisplayText = isEditingDelete ? editingText : deleteIndexValue;
        
        hoveringDeleteInputField = RenderUtils.isHovering(
            mouseX, mouseY, 
            (int)x, (int)(deleteY + 15), 
            (int)(x + 100), (int)(deleteY + 35)
        );
        
        int deleteInputFieldColor = hoveringDeleteInputField ? Colors.getColor(40, 40, 40, 200) : Colors.getColor(30, 30, 30, 200);
        RenderUtils.drawRoundedRect(stack, x, deleteY + 15, 100, 20, 3, deleteInputFieldColor);
        
        // 渲染删除索引文本
        if (!deleteDisplayText.isEmpty()) {
            opensans.render(stack, deleteDisplayText, (double)(x + 5), (double)(deleteY + 20), Color.LIGHT_GRAY, true, 0.35);
        }
        
        // 渲染删除歌曲光标（如果正在编辑且光标可见）
        if (isEditingDelete && cursorVisible) {
            // 计算光标位置
            float cursorX = x + 5 + opensans.getWidth(deleteDisplayText.substring(0, Math.min(cursorPosition, deleteDisplayText.length())), 0.35);
            // 绘制光标
            RenderUtils.fill(stack, cursorX, deleteY + 18, cursorX + 1, deleteY + 32, Color.WHITE.getRGB());
        }
        
        // 渲染删除歌曲按钮
        hoveringDeleteConfirmButton = RenderUtils.isHovering(
            mouseX, mouseY, 
            (int)(x + 110), (int)(deleteY + 15), 
            (int)(x + 170), (int)(deleteY + 35)
        );
        
        int deleteButtonColor = hoveringDeleteConfirmButton ? Colors.getColor(70, 70, 70, 200) : Colors.getColor(50, 50, 50, 200);
        RenderUtils.drawRoundedRect(stack, x + 110, deleteY + 15, 60, 20, 3, deleteButtonColor);
        opensans.render(stack, "确定", (double)(x + 115), (double)(deleteY + 20), Color.WHITE, true, 0.35);
        
        // 渲染删除状态消息
        String deleteStatusMessage = musicPlayer.getDeleteStatusMessageValue().getCurrentValue();
        if (deleteStatusMessage != null && !deleteStatusMessage.isEmpty()) {
            // 根据消息内容设置颜色（错误为红色，成功为绿色）
            Color deleteStatusColor = deleteStatusMessage.contains("失败") ? Color.RED : Color.GREEN;
            opensans.render(stack, deleteStatusMessage, (double)x, (double)(deleteY + 40), deleteStatusColor, true, 0.35);
        }
        
        // 渲染播放列表标题
        float playlistY = deleteY + 60;
        opensans.render(stack, "播放列表:", (double)x, (double)playlistY, Color.WHITE, true, 0.4);
        
        // 渲染播放列表
        List<String> musicList = musicPlayer.getMusicList();
        float listY = playlistY + 15;
        for (int i = 0; i < musicList.size() && i < 5; i++) { // 只显示前5首歌曲
            String songPath = musicList.get(i);
            String songName = new File(songPath).getName();
            
            // 如果是当前播放的歌曲，用不同颜色标记
            Color songColor = (musicPlayer.getCurrentSong() != null && musicPlayer.getCurrentSong().equals(songPath)) ? 
                Color.YELLOW : Color.LIGHT_GRAY;
            
            opensans.render(stack, (i+1) + ". " + songName, (double)(x + 5), (double)listY, songColor, true, 0.35);
            listY += 15;
        }
        
        // 如果歌曲数量超过5首，显示还有多少首
        if (musicList.size() > 5) {
            opensans.render(stack, "... 还有 " + (musicList.size() - 5) + " 首", (double)(x + 5), (double)listY, Color.GRAY, true, 0.35);
        }
        
        // 如果播放列表为空，显示提示信息
        if (musicList.isEmpty()) {
            opensans.render(stack, "播放列表为空", (double)(x + 5), (double)listY, Color.GRAY, true, 0.35);
        }
    }
    
    public boolean isHoveringAddButton() {
        return hoveringAddButton;
    }
    
    public boolean isHoveringPlayButton() {
        return hoveringPlayButton;
    }
    
    public boolean isHoveringNextButton() {
        return hoveringNextButton;
    }
    
    public boolean isHoveringClearButton() {
        return hoveringClearButton;
    }
    
    public boolean isHoveringDeleteConfirmButton() {
        return hoveringDeleteConfirmButton;
    }
    
    public boolean isHoveringInputField() {
        return hoveringInputField;
    }
    
    public boolean isHoveringDeleteInputField() {
        return hoveringDeleteInputField;
    }
    
    public void onAddButtonClicked() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastAddClickTime > CLICK_DELAY) {
            lastAddClickTime = currentTime;
            String path = musicPlayer.getMusicPathValue().getCurrentValue();
            if (path != null && !path.isEmpty()) {
                musicPlayer.addMusicToFileList(path);
            } else {
                musicPlayer.getStatusMessageValue().setCurrentValue("请输入文件路径！");
            }
        }
    }
    
    public void onPlayButtonClicked() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastPlayClickTime > CLICK_DELAY) {
            lastPlayClickTime = currentTime;
            if (musicPlayer.isEnabled()) {
                // 如果模块已启用，则禁用它（停止播放）
                musicPlayer.setEnabled(false);
            } else {
                // 如果模块未启用且播放列表不为空，则启用它（开始播放）
                if (!musicPlayer.getMusicList().isEmpty()) {
                    musicPlayer.setEnabled(true);
                } else {
                    musicPlayer.getStatusMessageValue().setCurrentValue("播放列表为空！");
                }
            }
        }
    }
    
    public void onNextButtonClicked() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastNextClickTime > CLICK_DELAY) {
            lastNextClickTime = currentTime;
            // 如果正在播放音乐，则停止当前音乐并播放下一首
            if (musicPlayer.isEnabled()) {
                musicPlayer.setEnabled(false); // 先停止当前播放
                // 延迟一点时间再启用，以确保当前音乐完全停止
                new Thread(() -> {
                    try {
                        Thread.sleep(100);
                        musicPlayer.setEnabled(true);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
            } else {
                // 如果没有播放，则启用模块开始播放
                if (!musicPlayer.getMusicList().isEmpty()) {
                    musicPlayer.setEnabled(true);
                } else {
                    musicPlayer.getStatusMessageValue().setCurrentValue("播放列表为空！");
                }
            }
        }
    }
    
    public void onClearButtonClicked() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastClearClickTime > CLICK_DELAY) {
            lastClearClickTime = currentTime;
            musicPlayer.clearPlaylist();
        }
    }
    
    public void onDeleteConfirmButtonClicked() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastDeleteConfirmClickTime > CLICK_DELAY) {
            lastDeleteConfirmClickTime = currentTime;
            String indexStr = musicPlayer.getDeleteSongIndexValue().getCurrentValue();
            if (indexStr != null && !indexStr.isEmpty()) {
                musicPlayer.deleteSongByIndex(indexStr);
            } else {
                musicPlayer.getDeleteStatusMessageValue().setCurrentValue("请输入歌曲编号！");
            }
        }
    }
}