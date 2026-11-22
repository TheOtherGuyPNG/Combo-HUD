package combo;

import com.google.gson.Gson;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;

public class Combo implements ClientModInitializer {
    static class Element {
        double x=.14, y=.32;
        float scale=2; int color=15;
        boolean visible=true;
    }
    static class Settings {
        boolean enabled=true, zero=true, playersOnly=true;
        int timeoutSeconds=3;
        Element label=new Element(), count=new Element();
        Settings(){count.y=.43; count.color=12;}
    }
    static final int[] colors={0x000000,0x0000AA,0x00AA00,0x00AAAA,0xAA0000,0xAA00AA,0xFFAA00,0xAAAAAA,0x555555,0x5555FF,0x55FF55,0x55FFFF,0xFF5555,0xFF55FF,0xFFFF55,0xFFFFFF};
    static final Path path=FabricLoader.getInstance().getConfigDir().resolve("combo-hud.json");
    static Settings cfg=new Settings();
    static final Gson gson=new Gson();
    static int combo;
    static LocalPlayer owner;
    static Object level;
    static final Map<Integer,Integer> swings=new HashMap<>();
    static long lastHit;
    static final Set<Integer> damagedTargets=new HashSet<>();
    static void endCombo(){combo=0;damagedTargets.clear();}
    static void reset(){endCombo();swings.clear();}
    static void sync(){
        var mc=Minecraft.getInstance();
        if(owner!=mc.player || level!=mc.level){reset();owner=mc.player;level=mc.level;}
        if(combo>0 && System.nanoTime()-lastHit>=cfg.timeoutSeconds*1_000_000_000L)endCombo();
    }
    public static void attack(Player attacker,Entity target){
        sync();
        if(!cfg.enabled || attacker!=owner || !(target instanceof LivingEntity) || (cfg.playersOnly && !(target instanceof Player))) return;
        swings.values().removeIf(t->owner.tickCount-t>60);
        if(attacker.getMainHandItem().is(ItemTags.SWORDS)) swings.put(target.getId(),owner.tickCount);
        else swings.remove(target.getId());
    }
    public static void damage(ClientboundDamageEventPacket packet){
        sync();
        if(!cfg.enabled || owner==null) return;
        if(packet.entityId()==owner.getId()){reset();return;}
        var source=packet.getSource(owner.level());
        damagedTargets.remove(packet.entityId());
        if(source.getEntity()!=owner || source.getDirectEntity()!=owner) return;
        Integer tick=swings.remove(packet.entityId());
        if(tick!=null && owner.tickCount-tick<=60){
            combo++;
            lastHit=System.nanoTime();
            damagedTargets.add(packet.entityId());
        }
    }
    public static void entityEvent(net.minecraft.network.protocol.game.ClientboundEntityEventPacket packet){
        if(packet.getEventId()!=EntityEvent.DEATH)return;
        sync();
        if(owner==null)return;
        Entity victim=packet.getEntity(owner.level());
        if(victim!=null && damagedTargets.contains(victim.getId()))reset();
    }
    static void save(){
        try {
            Path tmp=Files.createTempFile(path.getParent(),"combo-hud",".tmp");
            Files.writeString(tmp,gson.toJson(cfg));
            Files.move(tmp,path,StandardCopyOption.REPLACE_EXISTING);
        } catch(IOException e){System.getLogger("Combo HUD").log(System.Logger.Level.WARNING,"Could not save HUD settings",e);}
    }
    static Element el(int i){return i==0?cfg.label:cfg.count;}
    static int pos(double fraction,int room){return (int)Math.round(Math.clamp(fraction,0,1)*Math.max(0,room));}
    static int ew(int i,String s){return Math.round(Minecraft.getInstance().font.width(s)*el(i).scale)+6;}
    static int eh(int i){return Math.round(9*el(i).scale)+6;}
    static void draw(GuiGraphicsExtractor g,int i,String s,boolean edit){
        Element e=el(i);
        if(!e.visible && !edit)return;
        int w=ew(i,s),h=eh(i),x=pos(e.x,g.guiWidth()-w),y=pos(e.y,g.guiHeight()-h);
        if(edit)g.outline(x,y,w,h,e.visible?0xffff8822:0xff888888);
        var pose=g.pose();
        pose.pushMatrix();pose.translate(x+3,y+3);pose.scale(e.scale,e.scale);
        g.text(Minecraft.getInstance().font,s,0,0,0xff000000|colors[e.color],true);
        pose.popMatrix();
    }
    public void onInitializeClient(){
        if(Files.exists(path))try{
            Settings loaded=gson.fromJson(Files.readString(path),Settings.class);
            if(loaded!=null && loaded.label!=null && loaded.count!=null)cfg=loaded;
        }catch(Exception ignored){}
        cfg.timeoutSeconds=Math.clamp(cfg.timeoutSeconds,1,10);
        for(int i=0;i<2;i++){var e=el(i);e.color=Math.clamp(e.color,0,15);e.scale=Math.clamp(e.scale,0.5f,6);}
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("combo_hud","counter"),(g,t)->{
            sync();
            var mc=Minecraft.getInstance();
            if(owner==null || mc.gui.screen() instanceof Menu || !cfg.enabled || (!cfg.zero && combo==0))return;
            draw(g,0,"Combo",false);draw(g,1,combo+"x",false);
        });
    }
}




