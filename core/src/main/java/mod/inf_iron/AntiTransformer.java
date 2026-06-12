package mod.inf_iron;

import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.ITransformerVotingContext;
import cpw.mods.modlauncher.api.ITransformer.Target;
import cpw.mods.modlauncher.api.TransformerVoteResult;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import java.util.Set;
import com.google.common.collect.ImmutableSet;

public class AntiTransformer implements ITransformer<ClassNode> {

    @Override
    public Set<Target> targets() {
        return ImmutableSet.of(Target.targetClass("net.minecraft.world.entity.Entity"));
    }

    @Override
    public ClassNode transform(ClassNode node, ITransformerVotingContext context) { 
        for (MethodNode method : node.methods) {
            // 1.20.1の discard() のSRG名は m_142685_
            if (method.name.equals("discard") || method.name.equals("m_142685_")) {
                method.instructions.clear();
                InsnList insns = new InsnList();
                insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
                insns.add(new FieldInsnNode(Opcodes.GETSTATIC, "net/minecraft/world/entity/Entity$RemovalReason", "DISCARDED", "Lnet/minecraft/world/entity/Entity$RemovalReason;"));
                // 内部で呼び出す setRemoved() のSRG名が m_142687_
                insns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "net/minecraft/world/entity/Entity", "m_142687_", "(Lnet/minecraft/world/entity/Entity$RemovalReason;)V", false));
                insns.add(new InsnNode(Opcodes.RETURN));
                method.instructions.add(insns);
                method.maxStack = 2;
                method.maxLocals = 1;
            }
        }
        return node;
    }

    @Override
    public TransformerVoteResult castVote(ITransformerVotingContext context) {
        return TransformerVoteResult.YES;
    }
}
