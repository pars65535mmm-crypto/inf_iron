package mod.inf_iron;

import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.ITransformer;
import java.util.List;
import java.util.Set;

public class InfIronTransformerService implements ITransformationService {
    @Override
    public String name() { return "inf_iron_transformer"; }

    @Override
    public void initialize(IEnvironment environment) {} 

    @Override
    public void onLoad(IEnvironment env, Set<String> allServices) {}

    @Override
    public List<ITransformer> transformers() {
        return List.of(new AntiTransformer()); // ここを修正！
    }
}