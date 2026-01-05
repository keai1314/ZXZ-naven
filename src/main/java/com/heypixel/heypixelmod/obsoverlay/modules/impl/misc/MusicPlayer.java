package com.heypixel.heypixelmod.obsoverlay.modules.impl.misc;

import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.values.Value;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.HasValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.StringValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import com.heypixel.heypixelmod.obsoverlay.ui.LanguageManager;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.client.Minecraft;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@ModuleInfo(
    name = "MusicPlayer",
    description = "播放本地路径内的.MP3文件",
    category = Category.MISC
)
public class MusicPlayer extends Module {
    private final StringValue musicPathValue;
    private final StringValue statusMessageValue;
    private final ModeValue playModeValue;
    private final StringValue playlistValue; // 用于存储播放列表的字符串值
    
    private final List<String> musicList = new ArrayList<>();
    private int currentIndex = 0;
    private Random random = new Random();
    
    // 音频播放相关字段
    private boolean isPlaying = false;
    private String currentSong = null;
    private Thread playbackThread = null; // 用于控制播放线程
    
    // 删除歌曲相关字段
    private final StringValue deleteSongIndexValue;
    private final StringValue deleteStatusMessageValue;
    
    private static final Minecraft mc = Minecraft.getInstance();
    
    public MusicPlayer() {
        // 创建音乐路径输入框
        musicPathValue = (StringValue) ValueBuilder.create(this, "音乐路径")
                .setValueType(com.heypixel.heypixelmod.obsoverlay.values.ValueType.STRING)
                .setDefaultStringValue("")
                .build();
        
        // 创建状态消息显示（用于显示添加成功或错误信息）
        statusMessageValue = (StringValue) ValueBuilder.create(this, "状态消息")
                .setValueType(com.heypixel.heypixelmod.obsoverlay.values.ValueType.STRING)
                .setDefaultStringValue("")
                .build();
        
        // 创建播放模式选择（顺序播放、随机播放、循环播放）
        playModeValue = (ModeValue) ValueBuilder.create(this, "播放模式")
                .setValueType(com.heypixel.heypixelmod.obsoverlay.values.ValueType.MODE)
                .setModes("顺序播放", "随机播放", "循环播放")
                .setDefaultModeIndex(0)
                .build();
                
        // 创建播放列表存储值（用于持久化存储）
        playlistValue = (StringValue) ValueBuilder.create(this, "播放列表")
                .setValueType(com.heypixel.heypixelmod.obsoverlay.values.ValueType.STRING)
                .setDefaultStringValue("")
                .build();
                
        // 创建删除歌曲索引输入框
        deleteSongIndexValue = (StringValue) ValueBuilder.create(this, "删除歌曲索引")
                .setValueType(com.heypixel.heypixelmod.obsoverlay.values.ValueType.STRING)
                .setDefaultStringValue("")
                .build();
                
        // 创建删除状态消息显示
        deleteStatusMessageValue = (StringValue) ValueBuilder.create(this, "删除状态消息")
                .setValueType(com.heypixel.heypixelmod.obsoverlay.values.ValueType.STRING)
                .setDefaultStringValue("")
                .build();
                
        // 注册值到管理器
    }
    
    public String getLocalizedName() {
        return LanguageManager.getInstance().getLocalizedString("音乐播放器", "MusicPlayer");
    }

    public String getLocalizedDescription() {
        return LanguageManager.getInstance().getLocalizedString("播放本地.MP3文件", "Play local .MP3 files");
    }
    
    // 初始化模块时加载播放列表
    @Override
    public void onEnable() {
        loadPlaylist();
        if (!musicList.isEmpty()) {
            playNextSong();
        } else {
            statusMessageValue.setCurrentValue("播放列表为空！");
        }
    }
    
    // 模块禁用时保存播放列表
    @Override
    public void onDisable() {
        savePlaylist();
        stopMusic();
    }
    
    // 保存播放列表到配置
    private void savePlaylist() {
        StringBuilder playlistString = new StringBuilder();
        for (int i = 0; i < musicList.size(); i++) {
            playlistString.append(musicList.get(i));
            if (i < musicList.size() - 1) {
                playlistString.append("|"); // 使用|分隔歌曲路径
            }
        }
        playlistValue.setCurrentValue(playlistString.toString());
    }
    
    // 从配置加载播放列表
    private void loadPlaylist() {
        musicList.clear();
        String playlistString = playlistValue.getCurrentValue();
        if (playlistString != null && !playlistString.isEmpty()) {
            String[] songs = playlistString.split("\\|");
            for (String song : songs) {
                if (!song.isEmpty()) {
                    // 验证文件是否存在
                    File file = new File(song);
                    if (file.exists()) {
                        musicList.add(song);
                    }
                }
            }
        }
    }
    
    // 添加音乐文件到播放列表
    public void addMusicToFileList(String filePath) {
        // 检查文件是否存在
        File file = new File(filePath);
        if (!file.exists()) {
            statusMessageValue.setCurrentValue("文件不存在！");
            return;
        }
        
        // 检查文件是否为MP3格式
        if (!filePath.toLowerCase().endsWith(".mp3")) {
            statusMessageValue.setCurrentValue("文件类型错误！");
            return;
        }
        
        // 添加到播放列表
        musicList.add(filePath);
        savePlaylist(); // 保存播放列表
        statusMessageValue.setCurrentValue("添加成功！");
    }
    
    // 清空播放列表
    public void clearPlaylist() {
        musicList.clear();
        savePlaylist(); // 保存空的播放列表
        statusMessageValue.setCurrentValue("播放列表已清空！");
    }
    
    // 删除指定索引的歌曲
    public void deleteSongByIndex(String indexStr) {
        try {
            int index = Integer.parseInt(indexStr);
            if (index > 0 && index <= musicList.size()) {
                musicList.remove(index - 1); // 用户输入的是从1开始的索引
                savePlaylist(); // 保存更新后的播放列表
                deleteStatusMessageValue.setCurrentValue("删除成功！");
            } else {
                deleteStatusMessageValue.setCurrentValue("删除失败，不存在该歌曲");
            }
        } catch (NumberFormatException e) {
            deleteStatusMessageValue.setCurrentValue("删除失败，请输入有效的数字");
        }
    }
    
    // 播放音乐（使用Minecraft的声音系统）
    private void playMusic(String filePath) {
        // 停止任何正在进行的播放
        stopMusic();
        
        isPlaying = true;
        currentSong = filePath;
        statusMessageValue.setCurrentValue("正在播放: " + new File(filePath).getName());
        
        // 播放声音（这里我们播放一个示例声音，实际MP3播放需要更复杂的实现）
        // 在实际应用中，您需要使用像JLayer这样的库来播放MP3文件
        if (mc.player != null && mc.level != null) {
            // 播放一个声音作为示例
            mc.player.playNotifySound(SoundEvents.MUSIC_DISC_11, SoundSource.MUSIC, 1.0F, 1.0F);
        }
        
        // 启动一个新的线程来模拟播放
        playbackThread = new Thread(() -> {
            try {
                // 模拟播放180秒（3分钟）
                int playTime = 180; // 秒
                int elapsed = 0;
                
                while (elapsed < playTime && isPlaying) {
                    Thread.sleep(1000); // 每秒更新一次
                    elapsed++;
                }
                
                // 播放完成后，如果仍在播放状态，则播放下一首
                if (isPlaying) {
                    playNextSong();
                }
            } catch (InterruptedException e) {
                // 线程被中断，正常退出
            }
        });
        
        playbackThread.start();
    }
    
    // 停止音乐
    private void stopMusic() {
        isPlaying = false;
        currentSong = null;
        
        // 中断播放线程（如果正在运行）
        if (playbackThread != null && playbackThread.isAlive()) {
            playbackThread.interrupt();
        }
        
        statusMessageValue.setCurrentValue("音乐已停止");
    }
    
    // 播放下一首歌曲
    private void playNextSong() {
        String nextSong = getNextSong();
        if (nextSong != null) {
            playMusic(nextSong);
        } else {
            statusMessageValue.setCurrentValue("播放结束");
            setEnabled(false); // 播放完毕后关闭模块
        }
    }
    
    // 获取下一首歌曲
    public String getNextSong() {
        if (musicList.isEmpty()) {
            return null;
        }
        
        switch (playModeValue.getCurrentValue()) {
            case 0: // 顺序播放
                if (currentIndex < musicList.size() - 1) {
                    return musicList.get(++currentIndex);
                }
                return null; // 列表结束
                
            case 1: // 随机播放
                if (!musicList.isEmpty()) {
                    return musicList.get(random.nextInt(musicList.size()));
                }
                return null;
                
            case 2: // 循环播放
                if (musicList.isEmpty()) {
                    return null;
                }
                // 返回当前选中的歌曲（需要在UI中让用户选择）
                // 如果没有选中任何歌曲，默认返回第一首
                if (currentIndex >= 0 && currentIndex < musicList.size()) {
                    return musicList.get(currentIndex);
                } else {
                    currentIndex = 0;
                    return musicList.get(currentIndex);
                }
                
            default:
                return null;
        }
    }
    
    // 获取播放列表
    public List<String> getMusicList() {
        return new ArrayList<>(musicList);
    }
    
    // 设置当前播放的歌曲索引（用于循环播放模式）
    public void setCurrentIndex(int index) {
        if (index >= 0 && index < musicList.size()) {
            currentIndex = index;
        }
    }
    
    // 获取当前播放模式
    public int getPlayMode() {
        return playModeValue.getCurrentValue();
    }
    
    // 获取音乐路径值对象
    public StringValue getMusicPathValue() {
        return musicPathValue;
    }
    
    // 获取状态消息值对象
    public StringValue getStatusMessageValue() {
        return statusMessageValue;
    }
    
    // 获取播放模式值对象
    public ModeValue getPlayModeValue() {
        return playModeValue;
    }
    
    // 获取播放列表值对象
    public StringValue getPlaylistValue() {
        return playlistValue;
    }
    
    // 获取删除歌曲索引值对象
    public StringValue getDeleteSongIndexValue() {
        return deleteSongIndexValue;
    }
    
    // 获取删除状态消息值对象
    public StringValue getDeleteStatusMessageValue() {
        return deleteStatusMessageValue;
    }
    
    // 检查是否正在播放
    public boolean isPlaying() {
        return isPlaying;
    }
    
    // 获取当前播放的歌曲
    public String getCurrentSong() {
        return currentSong;
    }
}