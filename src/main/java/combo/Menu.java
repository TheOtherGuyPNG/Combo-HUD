package combo;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import static combo.Combo.*;

public class Menu extends Screen {
    final Screen parent;
    final int page;
    int selected, drag=-1, split;
    double grabX,grabY,pressX,pressY;
    int anchorX,anchorY;
    float initialScale;
    boolean resizing;
    static final int HANDLE=7;
    static final String[] labels={"Toggle Mod","Color Editor","HUD Editor","Display HUD at 0x","Display HUD for only players","Stop combo after (seconds)"};
    static final String[] help={"Track landed sword hits. Turning this off hides the HUD and clears your combo.","Choose separate colors for the Combo label and counter.","Move, resize, or toggle each element using a 3x preview.","Show both enabled elements at zero. Turn this off to show them only during a combo.","ON counts hits against players. OFF also counts hits against other living targets.","Enter 1 to 10 seconds. Each landed sword hit restarts the timer."};
    static final String[] names={"Black","Dark Blue","Dark Green","Dark Aqua","Dark Red","Dark Purple","Gold","Gray","Dark Gray","Blue","Green","Aqua","Red","Light Purple","Yellow","White"};
    final Button[] rows=new Button[5];
    EditBox seconds;
    Menu(Screen parent,int page){super(Component.literal(new String[]{"Combo HUD","Color Editor","HUD Editor"}[page]));this.parent=parent;this.page=page;}
    Button button(String text,int x,int y,int w,Runnable action){
        return addRenderableWidget(Button.builder(Component.literal(text),b->action.run()).bounds(x,y,w,20).build());
    }
    String value(int i){return switch(i){case 0->cfg.enabled?"ON":"OFF";case 4->cfg.playersOnly?"ON":"OFF";case 3->cfg.zero?"ON":"OFF";default->"EDIT";};}
    void open(Screen s){minecraft.gui.setScreen(s);}
    public void onClose(){save();open(parent);}
    protected void init(){
        split=width*2/3;
        if(page==0){
            for(int i=0;i<rows.length;i++){
                final int n=i;
                rows[i]=button("",8,32+24*i,split-16,()->{
                    switch(n){
                        case 0->{cfg.enabled=!cfg.enabled;reset();}
                        case 3->cfg.zero=!cfg.zero;
                        case 4->{cfg.playersOnly=!cfg.playersOnly;reset();}
                        default->{open(new Menu(this,n));return;}
                    }
                    save();rebuildWidgets();
                });
                rows[i].setMessage(Component.literal(labels[i]+" "+value(i)));
                rows[i].setAlpha(1f);
            }
            seconds=new EditBox(font,split-44,32+24*5,36,20,Component.literal(labels[5])){
                @Override public void insertText(String input){
                    if(!input.matches("[0-9]+"))return;
                    String before=getValue();
                    int cursor=getCursorPosition();
                    super.insertText(input);
                    if(!getValue().matches("[1-9]|10")){
                        setValue(before);
                        setCursorPosition(cursor);
                        setHighlightPos(cursor);
                    }
                }
                @Override public void setFocused(boolean focused){
                    super.setFocused(focused);
                    if(!focused && getValue().isEmpty())setValue(Integer.toString(cfg.timeoutSeconds));
                }
            };
            seconds.setValue(Integer.toString(cfg.timeoutSeconds));
            seconds.setResponder(v->{if(v.matches("[1-9]|10"))cfg.timeoutSeconds=Integer.parseInt(v);});
            addRenderableWidget(seconds);
            button("Done",split+6,height-26,width-split-12,this::onClose);
        }else{
            int bw=Math.min(180,(width-20)/2);
            for(int i=0;i<2;i++){
                final int n=i;
                button((selected==i?"> ":"")+(i==0?"Combo label":"Counter"),width/2-bw+ i*bw,4,bw,()->{selected=n;rebuildWidgets();});
            }
            if(page==1){
                int cell=Math.min(100,(width-16)/4);
                for(int i=0;i<16;i++){
                    final int n=i;
                    button((el(selected).color==i?"* ":"")+names[i],width/2-2*cell+i%4*cell,36+i/4*23,cell,()->{el(selected).color=n;save();rebuildWidgets();});
                }
            }else{
                int x=width/2-95;
                button("-",x,height-48,26,()->scale(-1));
                button("+",x+28,height-48,26,()->scale(1));
                button(el(selected).visible?"Visible: ON":"Visible: OFF",x+56,height-48,134,()->{el(selected).visible=!el(selected).visible;save();rebuildWidgets();});
            }
            button("Done",width/2-45,height-23,90,this::onClose);
        }
    }
    void scale(int step){el(selected).scale=Math.clamp(el(selected).scale+step,1,6);save();}
    public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
        super.extractRenderState(g,mx,my,delta);
        if(page==0){
            g.fill(split,0,split+1,height,0xff777777);
            g.centeredText(font,"Combo HUD",split/2,12,-1);
            int active=-1;
            for(int i=0;i<rows.length;i++){
                Button b=rows[i];
                if(b.isHovered())active=i;
                else if(active<0 && b.isFocused())active=i;
                int y=b.getY();
                int fill=b.isHoveredOrFocused()?0xff777777:0xff515151;
                g.fill(10,y+2,split-10,y+18,fill);
                g.text(font,labels[i],13,y+6,-1);
                g.text(font,value(i),split-13-font.width(value(i)),y+6,-1);
            }
            int timerY=32+24*5;
            g.text(font,labels[5],13,timerY+6,-1);
            if((mx>=8 && mx<split && my>=timerY && my<timerY+20) || (active<0 && seconds.isFocused()))active=5;
            g.text(font,active<0?"Combo HUD":labels[active],split+6,12,-1);
            if(active>=0)g.textWithWordWrap(font,Component.literal(help[active]),split+6,32,width-split-12,-1);
        }else if(page==1){
            for(int i=0;i<16;i++){
                int cell=Math.min(100,(width-16)/4),x=width/2-2*cell+i%4*cell+3,y=39+i/4*23;
                g.fill(x,y,x+3,y+14,0xff000000|colors[i]);
            }
        }else{
            g.centeredText(font,"Drag text to move  |  Drag a corner square to resize",width/2,27,-1);
            for(int layer=0;layer<2;layer++){
                int i=layer==0?1-selected:selected;
                String text=i==0?"Combo":"3x";
                draw(g,i,text,true);
                int w=ew(i,text),h=eh(i);
                int right=pos(el(i).x,width-w)+w,bottom=pos(el(i).y,height-h)+h;
                g.fill(right-HANDLE,bottom-HANDLE,right,bottom,0xffff8822);
                g.outline(right-HANDLE,bottom-HANDLE,HANDLE,HANDLE,0xff292019);
            }
        }
    }
    public boolean mouseClicked(MouseButtonEvent e,boolean twice){
        if(super.mouseClicked(e,twice))return true;
        if(page!=2 || e.button()!=org.lwjgl.sdl.SDLMouse.SDL_BUTTON_LEFT)return false;
        for(int layer=0;layer<2;layer++){
            int i=layer==0?selected:1-selected;
            int w=ew(i,i==0?"Combo":"3x"),h=eh(i);
            int x=pos(el(i).x,width-w),y=pos(el(i).y,height-h);
            if(e.x()>=x && e.x()<x+w && e.y()>=y && e.y()<y+h){
                selected=drag=i;
                pressX=e.x();pressY=e.y();anchorX=x;anchorY=y;
                grabX=pressX-x;grabY=pressY-y;initialScale=el(i).scale;
                resizing=grabX>=w-HANDLE && grabY>=h-HANDLE;
                setDragging(true);
                return true;
            }
        }
        return false;
    }
    public boolean mouseDragged(MouseButtonEvent e,double dx,double dy){
        if(drag<0)return super.mouseDragged(e,dx,dy);
        var item=el(drag);
        double x=e.x()-grabX,y=e.y()-grabY;
        if(resizing){
            int textWidth=font.width(drag==0?"Combo":"3x");
            double change=((e.x()-pressX)*textWidth+(e.y()-pressY)*9)/(textWidth*textWidth+81);
            item.scale=(float)Math.clamp(initialScale+change,0.5,6);
            x=anchorX;y=anchorY;
        }
        item.x=Math.clamp(x/Math.max(1,width-ew(drag,drag==0?"Combo":"3x")),0,1);
        item.y=Math.clamp(y/Math.max(1,height-eh(drag)),0,1);
        return true;
    }
    public boolean mouseReleased(MouseButtonEvent e){
        if(drag<0 || e.button()!=org.lwjgl.sdl.SDLMouse.SDL_BUTTON_LEFT)return super.mouseReleased(e);
        drag=-1;setDragging(false);save();rebuildWidgets();return true;
    }
}




