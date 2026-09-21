package com.piranport.combat.neural;
import org.junit.jupiter.api.Test;
import java.lang.reflect.*;
class NetProbeTest {
    @Test void probe() throws Exception {
        BallisticNet net = BallisticNet.loadExperimental();
        Field lf = BallisticNet.class.getDeclaredField("layers"); lf.setAccessible(true);
        Object[] layers = (Object[]) lf.get(net);
        Field wf = layers[0].getClass().getDeclaredField("weights"); wf.setAccessible(true);
        Field bf = layers[0].getClass().getDeclaredField("biases"); bf.setAccessible(true);
        Field inf = layers[0].getClass().getDeclaredField("inDim"); inf.setAccessible(true);
        Field of = layers[0].getClass().getDeclaredField("outDim"); of.setAccessible(true);
        Field rf = layers[0].getClass().getDeclaredField("relu"); rf.setAccessible(true);
        double[] w=(double[])wf.get(layers[0]); double[] b=(double[])bf.get(layers[0]);
        int ni=(int)inf.get(layers[0]), no=(int)of.get(layers[0]);
        Field xmf = BallisticNet.class.getDeclaredField("xMean"); xmf.setAccessible(true);
        Field xsf = BallisticNet.class.getDeclaredField("xStd"); xsf.setAccessible(true);
        double[] xm=(double[])xmf.get(net), xs=(double[])xsf.get(net);
        double G=9.8/196.0;
        double[] raw={54,0,3.0,0.01,G};
        double[] x=new double[5];
        for(int j=0;j<5;j++) x[j]=(raw[j]-xm[j])/xs[j];
        System.out.printf("NORMIN %.17g,%.17g,%.17g,%.17g,%.17g%n",x[0],x[1],x[2],x[3],x[4]);
        // 手工算第一层前 4 个输出
        for(int i=0;i<4;i++){
            double s=b[i]; int base=i*ni;
            for(int j=0;j<ni;j++) s+=w[base+j]*x[j];
            System.out.printf("L0OUT[%d] raw=%.17g relu=%.17g%n", i, s, s<0?0.0:s);
        }
    }
}
