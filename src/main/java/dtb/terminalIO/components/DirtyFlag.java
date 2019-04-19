package dtb.terminalIO.components;

public interface DirtyFlag {

    boolean isDirty();

    void markAsDirty();
    
//    void markAsClean();
    

//    public static class DefaultImpl implements DirtyFlag {
//
//        private DirtyFlag  owner;
//        private boolean dirty = false;
//        
//        @Override
//        public boolean isDirty() {
//            return dirty;
//        }
//
//        @Override
//        public void markAsDirty() {
//            if (owner.isDirty()) {
//                return;
//            }
//            dirty = false;
//            if (this.getParentContainer()!=null && this.getParentContainer() instanceof Component) {
//                if ( ! ((Component<?>)this.getParentContainer()).isDirty()) {
//                   ((Component<?>)this.getParentContainer()).markAsDirty();
//                }
//            }
//        }
//
//        @Override
//        public void markAsClean() {
//            // TODO Auto-generated method stub
//            
//        }
//        
//    }
    
    
}
