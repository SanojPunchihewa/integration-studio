package org.wso2.integrationstudio.carbonserver44microei42.wizard;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.wso2.integrationstudio.carbonserver44microei42.Activator;
import org.wso2.integrationstudio.carbonserver44microei42.register.product.servers.MicroIntegratorInstance;
import org.wso2.integrationstudio.carbonserver44microei42.util.ServerConstants;
import org.wso2.integrationstudio.distribution.project.model.DataTransferObject;
import org.wso2.integrationstudio.distribution.project.model.DependencyData;
import org.wso2.integrationstudio.distribution.project.model.NodeData;
import org.wso2.integrationstudio.distribution.project.util.DistProjectUtils;
import org.wso2.integrationstudio.distribution.project.validator.ProjectList;
import org.wso2.integrationstudio.logging.core.IIntegrationStudioLog;
import org.wso2.integrationstudio.logging.core.Logger;
import org.wso2.integrationstudio.maven.util.MavenUtils;
import org.wso2.integrationstudio.platform.core.model.AbstractListDataProvider.ListData;
import org.wso2.integrationstudio.platform.core.utils.SWTResourceManager;

public class DistributionProjectRuntimeWizardPage extends WizardPage {
    private static IIntegrationStudioLog log = Logger.getLog(Activator.PLUGIN_ID);
    
    private static final String METADATA_TYPE = "synapse/metadata";
    private static final String API_TYPE = "synapse/api";
    private static final String PROXY_SERVICE_TYPE = "synapse/proxy-service";
    private static final String METADATA_SUFFIX = "_metadata";
    private static final String SWAGGER_SUFFIX = "_swagger";
    private static final String PROXY_METADATA_SUFFIX = "_proxy";
    
    private MavenProject mavenProject;
    private Map<String,Dependency> dependencyList;
    private Map<String,DependencyData> projectList;
    private Map<String,Dependency> missingDependencyList;
    private Map<String,String> serverRoleList = new HashMap<String, String>();
    private Map<String, String> projectListToDependencyMapping = new LinkedHashMap<String, String>();
    private Map<String, String> artifactIdToDependencyMapping = new LinkedHashMap<String, String>();
    private Tree trDependencies;
    private TreeEditor editor;
    private Map<String,TreeItem>  nodesWithSubNodes = new HashMap<String,TreeItem>();
    private boolean pageDirty = false;
    private boolean controlCreated = false;
    private DataTransferObject dataTransferObject;
    private Composite container;
    private IFile selectedProjectPomFileRes;
    private IProject selectedCompositeProject;
    private String METADATA_FILE_TYPE = "yaml";

    // need to get the server roles via an extension point without hard-coding
    private final String[] serverRoles = new String[] { "BusinessProcessServer",
                    "EnterpriseServiceBus", "DataServicesServer",
                    "EnterpriseIntegrator" };
    
    public IProject getSelectedCompositeProject() {
        return selectedCompositeProject;
    }

    public IFile getSelectedProjectPomIFile() {
        return selectedProjectPomFileRes;
    }

    public MavenProject getMavenProject() {
        return mavenProject;
    }
    
    public Map<String, Dependency> getDependencyList() {
        return dependencyList;
    }

    public Map<String, DependencyData> getProjectList() {
        return projectList;
    }

    public Map<String, Dependency> getMissingDependencyList() {
        return missingDependencyList;
    }
    
    public void setDependencyList(Map<String, Dependency> dependencyList) {
        this.dependencyList = dependencyList;
    }

    public void setServerRoleList(Map<String,String> serverRoleList) {
        this.serverRoleList = serverRoleList;
    }

    public Map<String,String> getServerRoleList() {
        return serverRoleList;
    }

    public void setProjectList(Map<String, DependencyData> projectList) {
        this.projectList = projectList;
    }

    public void setMissingDependencyList(Map<String, Dependency> missingDependencyList) {
        this.missingDependencyList = missingDependencyList;
    }
    
    
    /**
     * Create the wizard.
     */
    public DistributionProjectRuntimeWizardPage(MavenProject project) {
        super("Runtime Selection");
        this.mavenProject = project;
        setTitle("Embedded Servers");
//        setDescription("Select a Runtime");
    }
    
    public DistributionProjectRuntimeWizardPage(MavenProject project, DataTransferObject dataObject) {
        super("Runtime Selection");
        this.dataTransferObject = dataObject;
        setTitle("Embedded Servers");
//        setDescription("Select a Runtime");
    }

    /**
     * Create contents of the wizard.
     * @param parent
     */
    public void createControl(Composite parent) {
        container = new Composite(parent, SWT.NULL);
        container.setLayout(new GridLayout(1, false));
        
//        Group group2 = new Group(container, SWT.SHADOW_IN);
////        group1.setText("Import an Embedded Server");
//        group2.setLayout(new RowLayout(SWT.HORIZONTAL));
//        new Label(group2, SWT.NONE).setText("Select archive file:");
//        Text text1 = new Text(group2, SWT.BORDER);
////        text1.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
//        new Button(group2, SWT.NONE).setText("Browse");
        
        Group group1 = new Group(container, SWT.SHADOW_IN);
        group1.setText("Available Embedded Servers");
        group1.setLayout(new RowLayout(SWT.VERTICAL));
        new Button(group1, SWT.RADIO).setText("Micro Integrator 4.2.0                               ");
        new Button(group1, SWT.RADIO).setText("Micro Integrator 4.2.0.12");
        new Button(group1, SWT.RADIO).setText("Micro Integrator 4.3.0");

        
        Label lblstaticText1 = new Label(container, SWT.NONE);
        lblstaticText1.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
        lblstaticText1.setText("Micro Integrator 4.2.0.12 will be used as the default embedded runtime");
//        
//        Label lblstaticText2 = new Label(container, SWT.NONE);
//        lblstaticText2.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
//        lblstaticText2.setText("Project Runtime version " + mavenProject.getProperties().getProperty("project.runtime.version"));
//        
//        Label lblstaticText3 = new Label(container, SWT.NONE);
//        lblstaticText3.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 4, 1));
//        
//        String serverName = "MI_" + mavenProject.getProperties().getProperty("project.runtime.version");
//        String runtimeName = "RUNTIME_" + mavenProject.getProperties().getProperty("project.runtime.version");
//        
//        if (MicroIntegratorInstance.getInstance().serverExists(serverName)) {
//            lblstaticText3.setText("Compatible runtime " + serverName + " already exists");
//        } else {
//            lblstaticText3.setText("No Compatible runtime found.");
//            
//            GridData gdBtn = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
//            gdBtn.widthHint = 200;
//            
//            Button btnSelectAll = new Button(container,SWT.NONE);
//            btnSelectAll.setText("Download and Update");
//            btnSelectAll.setLayoutData(gdBtn);
//            btnSelectAll.addListener(SWT.MouseDown, new Listener() {
//                public void handleEvent(Event evt) {
//                    MicroIntegratorInstance.getInstance().addNewServer(ServerConstants.WSO2_MI_110_RUNTIME, 
//                            ServerConstants.WSO2_MI_110_SERVER_TYPE, runtimeName, serverName);
//                    lblstaticText3.setText("New runtime " + serverName + " added");
//                    btnSelectAll.setEnabled(false);
//                    // setErrorMessage("Please tick/check atleast one artifact from the list");
//                }
//            });
//            
//        }
        
        setControl(container);
    }
    
    private void initializeProjectDetails() {
        try {

        } catch (Exception e) {
            log.error("Error while exporting meta data export in APIM Service catalog", e);
        }
    }
    
    private void loadMavenProjectDetails() {
        selectedCompositeProject = ResourcesPlugin.getWorkspace().getRoot()
                .getProject(dataTransferObject.getCompositeName());
        selectedProjectPomFileRes = selectedCompositeProject.getFile("pom.xml");
        File selectedProjectPomFile = selectedProjectPomFileRes.getLocation().toFile();

        ProjectList projectListProvider = new ProjectList();
        List<ListData> projectListData = projectListProvider.getListData(null, null);

        for (ListData data : projectListData) {
            DependencyData dependencyData = (DependencyData) data.getData();
            projectList.put(data.getCaption(), dependencyData);
        }

        try {
            mavenProject = MavenUtils.getMavenProject(selectedProjectPomFile);
            Map<String, Dependency> dependencyMap = new HashMap<String, Dependency>();
            Map<String, String> serverRoleList = new HashMap<String, String>();
            for (Dependency dependency : (List<Dependency>) mavenProject.getDependencies()) {
                dependencyMap.put(DistProjectUtils.getArtifactInfoAsString(dependency), dependency);
                serverRoleList.put(DistProjectUtils.getArtifactInfoAsString(dependency),
                        DistProjectUtils.getServerRole(mavenProject, dependency));
            }

            setProjectList(projectList);
            setDependencyList(dependencyMap);
            setMissingDependencyList(
                    (Map<String, Dependency>) ((HashMap<String, Dependency>) getDependencyList()).clone());
            setServerRoleList(serverRoleList);
        } catch (IOException e) {
            log.error("IOException and unable to load artifact details for user selected composite project", e);
        } catch (XmlPullParserException e) {
            log.error("XmlPullParserException and unable to load artifact details for user selected composite project",
                    e);
        }
    }
    
    /**
     * Update state of treeItem
     * @param item Selected tree item
     */
    private void updateCheckState(TreeItem item) {
        if (item != null) {
            TreeItem[] subItems = item.getItems();
            int i = 0;
            for (TreeItem subItem : subItems) {
                if (subItem.getChecked())
                    i++;
            }
            if (i == item.getItemCount()) {
                item.setGrayed(false);
                item.setChecked(true);
            } else if (i < item.getItemCount() && i > 0) {
                item.setChecked(true);
                item.setGrayed(true);

            } else {
                item.setGrayed(false);
                item.setChecked(false);
            }
            setPageDirtyState(true);
        }
    }
    
    
    private void setPageDirtyState(boolean value) {
        if (controlCreated){
            pageDirty = value;
        }
    }
    
    public boolean isPageDirty(){
        return pageDirty;
    }

    /**
     * Create content of tree control 
     */
    protected void createTreeContent() {
        trDependencies.removeAll();
        nodesWithSubNodes.clear();
        for (String project : getProjectList().keySet()) {
            DependencyData dependencyData = getProjectList().get(project);
            if (METADATA_TYPE.equals(dependencyData.getCApptype())) {
                continue;
            }
            Object parent = dependencyData.getParent();
            Object self = dependencyData.getSelf();
            if((parent==null) && (self!=null)){
                if(self instanceof IProject){
                    createNode(trDependencies, dependencyData.getDependency(), true);
                }
            } else if(parent!=null){
                if(parent instanceof IProject){
                    IProject prj = (IProject) parent;
                    TreeItem parentNode=null; 
                    if(nodesWithSubNodes.containsKey(prj.getName())){
                        parentNode = nodesWithSubNodes.get(prj.getName());
                    } else{
                        parentNode = createNode(trDependencies, prj);
                        nodesWithSubNodes.put(prj.getName(),parentNode);
                    }
                    if(parentNode!=null){
                        createNode(parentNode, dependencyData.getDependency(), true);
                    }
                    updateCheckState(parentNode);
                }
            }
        }

        if (getMissingDependencyList().size() > 0) {
            for (String dependency : getMissingDependencyList().keySet()) {
                Dependency missingDependency = getMissingDependencyList().get(dependency);
                if (!METADATA_FILE_TYPE.equals(missingDependency.getType())) {
                    createNode(trDependencies, missingDependency, false);
                }
            }
        }
        trDependencies.layout();
    }
    
    /**
     * Create a subItem for a project
     * @param parent Parent treeItem
     * @param project Project dependency
     * @param available available of dependency in workspace  
     * @return new TreeItem
     */
    TreeItem createNode(TreeItem parent, final Dependency project, boolean available){
        TreeItem item= new TreeItem(parent, SWT.NONE);
        String artifactInfo = DistProjectUtils.getArtifactInfoAsString(project);
        String serverRole = DistProjectUtils.getDefaultServerRole(getProjectList(),artifactInfo).replaceAll("^capp/","");
        String version = project.getVersion();
        
        item.setText(0,DistProjectUtils.getMavenInfoAsString(artifactInfo));
        
        item.setText(2,version);
        NodeData nodeData = new NodeData(project);
        nodeData.setServerRole(serverRole);
        item.setData(nodeData);
        
        if (getDependencyList().containsKey(artifactInfo)) {
            item.setChecked(true);
            String role = DistProjectUtils.getServerRole(mavenProject,getDependencyList().get(artifactInfo)).replaceAll("^capp/","");
            item.setText(1,role);
        } else{
            item.setText(1,serverRole);
        }
        
        if (getMissingDependencyList().containsKey(artifactInfo)) {
            getMissingDependencyList().remove(artifactInfo);
        }
        item.setImage(0, SWTResourceManager.getImage(this.getClass(),
        "/icons/artifact.png"));
    
        return item;
    }
    
    /**
     * Create a Item for a dependency
     * @param parent Parent tree control
     * @param project Project dependency
     * @param available available of dependency in workspace  
     * @return new TreeItem
     */
    TreeItem createNode(Tree parent, final Dependency project, boolean available){
        TreeItem item= new TreeItem(parent, SWT.NONE);
        final String artifactInfo = DistProjectUtils.getArtifactInfoAsString(project);
        final String serverRole = DistProjectUtils.getDefaultServerRole(getProjectList(),artifactInfo).replaceAll("^capp/","");
        final String version = project.getVersion();
        
        item.setText(0,DistProjectUtils.getMavenInfoAsString(artifactInfo));
        
        item.setText(2,version);
        
        NodeData nodeData = new NodeData(project);
        nodeData.setServerRole(serverRole);

        item.setData(nodeData);
        
        if(available) {
        if (getDependencyList().containsKey(artifactInfo)) {
            item.setChecked(true);
            String role = DistProjectUtils.getServerRole(mavenProject,getDependencyList().get(artifactInfo)).replaceAll("^capp/","");
            item.setText(1,role);
        } else{
            item.setText(1,serverRole);
        }
        if (getMissingDependencyList().containsKey(artifactInfo)) {
            getMissingDependencyList().remove(artifactInfo);
        }
        
    } else {
        if (getDependencyList().containsKey(artifactInfo)) {
            getDependencyList().remove(artifactInfo);
        }
    }
        if (available) {
        item.setImage(0, SWTResourceManager.getImage(this.getClass(),
                "/icons/artifact.png"));
    } else {
        item.setImage(0, SWTResourceManager.getImage(this.getClass(),
                "/icons/cancel_16.png"));
    }
        return item;
        
    }
    
    /**
     * Create a tree Item for a project
     * @param parent  Parent tree control
     * @param project eclipse project
     * @return  new TreeItem
     */
    TreeItem createNode(Tree parent, final IProject project){
        TreeItem item= new TreeItem(parent, SWT.NONE);
        MavenProject mavenProject;
        try {
            mavenProject = DistProjectUtils.getMavenProject(project);
            item.setText(0,project.getName());
            item.setText(1,"--");
            item.setText(2,mavenProject.getModel().getVersion());
            NodeData nodeData = new NodeData(project);
            item.setData(project);
            
            nodeData.setHaschildren(true);
            item.setData(nodeData);
            
            item.setImage(0, SWTResourceManager.getImage(this.getClass(),
            "/icons/projects.gif"));
        } catch (Exception e) {
         return null;
        }   
        return item;
        
    }
    
    /**
     * handle tree item check event
     * @param item Selected tree item
     */
    private void handleTreeItemChecked(TreeItem item){
        boolean select = item.getChecked();
        NodeData nodeData = (NodeData) item.getData();
        if (nodeData.hasChildren()) {
            TreeItem[] subItems = item.getItems();
            if (select) {
                boolean conflict=false;
                for (TreeItem subitem : subItems) {
                    if (!subitem.getChecked()) {
                        NodeData subNodeData = (NodeData) subitem.getData();
                        if(!isNameConflict(subNodeData)){
                            subitem.setChecked(true);
                            addDependency(subNodeData);
                        } else{
                            conflict=true;
                        }
                    }
                }
                if(conflict){
                    MessageDialog.openWarning(getShell(), "Add dependencies","Cannot add multiple dependencies with same identity");
                }
            } else {
                for (TreeItem subitem : subItems) {
                    if (subitem.getChecked()) {
                        subitem.setChecked(false);
                        NodeData subNodeData = (NodeData) subitem.getData();
                        removeDependency(subNodeData);
                    }
                }
            }
            updateCheckState(item);
        } else {
            TreeItem parentItem = item.getParentItem();
            if (select) {
                if(!isNameConflict(nodeData)){
                    addDependency(nodeData);
                } else{
                    item.setChecked(false);
                    MessageDialog.openWarning(getShell(), "Add dependencies","Cannot add multiple dependencies with same identity");
                }
            } else {
                removeDependency(nodeData);
            }
            updateCheckState(parentItem);
        }
    }
    
    /**
     * Check for conflicts
     * @param nodeData
     * @return
     */
    private boolean isNameConflict(NodeData nodeData){
        Dependency dependency = nodeData.getDependency();
        for(Dependency entry  : getDependencyList().values()){
            if(entry.getArtifactId().equalsIgnoreCase(dependency.getArtifactId())){
                return true;
            }
        }
        return false;
    }
    
    /**
     * handle tree item edit events
     * @param item Selected tree item
     */
    private void handleTreeItemEdit(final TreeItem item){
        final NodeData nodeData = (NodeData) item.getData();
        Control lastCtrl = editor.getEditor();
        if (lastCtrl != null){
            lastCtrl.dispose();  
        }
        if(nodeData.hasChildren() || !item.getChecked()) return;
        final String artifactInfo = DistProjectUtils
        .getArtifactInfoAsString(nodeData.getDependency());
        final Combo cmbServerRole = new Combo(trDependencies, SWT.SINGLE);
        cmbServerRole.setItems(serverRoles);
        cmbServerRole.setText(item.getText(1));
        cmbServerRole.setFocus();
        editor.setEditor(cmbServerRole , item,1);
        trDependencies.redraw();
        
        cmbServerRole.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
                String role = cmbServerRole.getText();
                item.setText(1, role);
                nodeData.setServerRole(role);
                if (getDependencyList().containsKey(artifactInfo)) {
                        serverRoleList.put(artifactInfo, "capp/" + role);
                        setPageDirtyState(true);
                }
            }
        });
//temporary commented out, eclipse freezes when handling dispose()
//      cmbServerRole.addFocusListener(new FocusAdapter() {
//          public void focusLost(FocusEvent e) {
//              String role = cmbServerRole.getText();
//              cmbServerRole.dispose();
//              item.setText(1, role);
//              nodeData.setServerRole(role);
//              if (getDependencyList().containsKey(artifactInfo)) {
//                  getDependencyList().get(artifactInfo).setScope("capp/" + role);
//              }
//          }
//      });
    }
    
    /**
     * Remove a project dependencies from the maven model
     * @param nodeData NodeData of selected treeitem
     */
    private void removeDependency(NodeData nodeData) {
        List<String> removingArtifacts = new ArrayList<>();
        Dependency project = nodeData.getDependency();
        String artifactInfo = DistProjectUtils.getArtifactInfoAsString(project);
        if (projectListToDependencyMapping.containsKey(artifactInfo)) {
            DependencyData dependencyData = projectList.get(projectListToDependencyMapping.get(artifactInfo));
            if (API_TYPE.equals(dependencyData.getCApptype())) {
                String medataName = project.getArtifactId() + METADATA_SUFFIX;
                String swaggerName = project.getArtifactId() + SWAGGER_SUFFIX;
                removingArtifacts.add(artifactIdToDependencyMapping.get(medataName));
                removingArtifacts.add(artifactIdToDependencyMapping.get(swaggerName));
            } else if (PROXY_SERVICE_TYPE.equals(dependencyData.getCApptype())) {
                String medataName = project.getArtifactId() + PROXY_METADATA_SUFFIX + METADATA_SUFFIX;
                removingArtifacts.add(artifactIdToDependencyMapping.get(medataName));
            }
        }
        removingArtifacts.add(artifactInfo);
        removeDependency(removingArtifacts);
    }
    
    private void removeDependency(List<String> artifacts) {
        for (String artifactInfo : artifacts) {
            if (getDependencyList().containsKey(artifactInfo)) {
                getDependencyList().remove(artifactInfo);
                if (serverRoleList.containsKey(artifactInfo)) {
                    serverRoleList.remove(artifactInfo);
                }
            }
        }
    }
    /**
     * Add a project dependencies to the maven model
     * @param nodeData NodeData of selected treeitem
     */
    private void addDependency(NodeData nodeData) {
        Dependency project = nodeData.getDependency();
        String serverRole = nodeData.getServerRole();
        String artifactInfo = DistProjectUtils.getArtifactInfoAsString(project);

        if (projectListToDependencyMapping.containsKey(artifactInfo)) {
            String dependencyMapping = projectListToDependencyMapping.get(artifactInfo);
            if (projectList.containsKey(dependencyMapping)) {
                DependencyData dependencyData = projectList.get(dependencyMapping);
                if (API_TYPE.equals(dependencyData.getCApptype())) {
                    String medataName = project.getArtifactId() + METADATA_SUFFIX;
                    String swaggerName = project.getArtifactId() + SWAGGER_SUFFIX;
                    String metadataArtifactInfo = artifactIdToDependencyMapping.get(medataName);
                    String swaggerArtifactInfo = artifactIdToDependencyMapping.get(swaggerName);
                    if (projectListToDependencyMapping.containsKey(metadataArtifactInfo)) {
                        Dependency metaDependency = projectList
                                .get(projectListToDependencyMapping.get(metadataArtifactInfo)).getDependency();
                        if (metaDependency != null && !getDependencyList().containsKey(metadataArtifactInfo)) {
                            serverRoleList.put(metadataArtifactInfo, "capp/" + serverRole);
                            getDependencyList().put(metadataArtifactInfo, metaDependency);
                        }
                    }
                    if (projectListToDependencyMapping.containsKey(swaggerArtifactInfo)) {
                        Dependency swaggerDependency = projectList
                                .get(projectListToDependencyMapping.get(swaggerArtifactInfo)).getDependency();
                        if (swaggerDependency != null && !getDependencyList().containsKey(swaggerArtifactInfo)) {
                            serverRoleList.put(swaggerArtifactInfo, "capp/" + serverRole);
                            getDependencyList().put(swaggerArtifactInfo, swaggerDependency);
                        }
                    }
                } else if (PROXY_SERVICE_TYPE.equals(dependencyData.getCApptype())) {
                    String medataName = project.getArtifactId() + PROXY_METADATA_SUFFIX + METADATA_SUFFIX;
                    String metadataArtifactInfo = artifactIdToDependencyMapping.get(medataName);
                    if (projectListToDependencyMapping.containsKey(metadataArtifactInfo)) {
                        Dependency metaDependency = projectList
                                .get(projectListToDependencyMapping.get(metadataArtifactInfo)).getDependency();
                        if (metaDependency != null && !getDependencyList().containsKey(metadataArtifactInfo)) {
                            serverRoleList.put(metadataArtifactInfo, "capp/" + serverRole);
                            getDependencyList().put(metadataArtifactInfo, metaDependency);
                        }
                    }
                }
            }
        }

        if (!getDependencyList().containsKey(artifactInfo)) {

            Dependency dependency = new Dependency();
            dependency.setArtifactId(project.getArtifactId());
            dependency.setGroupId(project.getGroupId());
            dependency.setVersion(project.getVersion());
            dependency.setType(project.getType());
            if(!serverRoleList.containsKey(artifactInfo)){
                serverRoleList.put(artifactInfo, "capp/" + serverRole);
            }
            getDependencyList().put(artifactInfo, dependency);
        }
    }
    
    private void validate() {
        if (getDependencyList().size() == 0) {
            setErrorMessage("Please tick/check atleast one artifact from the list");
            setPageComplete(false);
            return;
        } 
        setErrorMessage(null);
        setPageComplete(true);
    }
    
    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            if (dataTransferObject != null && dataTransferObject.getCompositeName() != null) {
                loadMavenProjectDetails();
                createTreeContent();
                validate();
            }
        }
        initializeProjectDetails();
        container.setVisible(visible);
    }
    
    @Override
    public IWizardPage getNextPage() {
        return super.getNextPage();
    }
}
