package combo;
import com.terraformersmc.modmenu.api.*;
public class Integration implements ModMenuApi {
    public ConfigScreenFactory<?> getModConfigScreenFactory(){return parent->new Menu(parent,0);}
}
