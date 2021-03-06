package scroll.examples.sync

import scroll.internal.Compartment
import scroll.examples.sync.roles.IConstructor
import scala.collection.mutable.ListBuffer

trait IConstructionCompartment extends Compartment {

  def getConstructorForClassName(classname: Object): IConstructor
  
  /**
   * 
   */
  protected def addManagerRoles(containers: ListBuffer[ConstructionContainer]): Unit = {
    containers.foreach { cc =>
      if (cc.isConstructed() && !cc.isStartElement()) {
        cc.getPlayerInstance() play cc.getManagerInstance()
      }
    }
  }

  /**
   * Add the delete roles for the elements in the ConstructionContainers.
   */
  protected def addDeleteRoles(containers: ListBuffer[ConstructionContainer]): Unit = {
    containers.foreach { cc =>
      if (cc.isConstructed()) {
        cc.getManagerInstance() play SynchronizationCompartment.destructionCompartment.getDestructorForClassName(cc.getPlayerInstance())
      }
    }
  }

  /**
   * Add the related RoleManagers for the elements in the ConstructionContainers.
   */
  protected def addRelatedRoleManager(containers: ListBuffer[ConstructionContainer]): Unit = {
    containers.foreach { cc =>
      containers.foreach { inner =>
        cc.getManagerInstance().addRelatedManager(inner.getManagerInstance())
      }
      //println("++++++++++++++++++++++++From: " + cc.managerInstance + " Rolemanagers: " + cc.managerInstance.relatedManager);
    }
  }

  /**
   * Combine the SynchronizationCompartment with all Players from the ConstructionContainers.
   */
  protected def synchronizeCompartments(containers: ListBuffer[ConstructionContainer]): Unit = {
    containers.foreach { cc =>
      SynchronizationCompartment combine cc.getPlayerInstance()
    }
  }

  /**
   * Create the Synchronization mechanisms for the elements in the ConstructionContainers.
   */
  protected def bindSynchronizationRules(containers: ListBuffer[ConstructionContainer]): Unit = {
    SynchronizationCompartment.syncCompartmentInfoList.foreach { s =>
      var sync: ISyncCompartment = null
      //Proof all container for integration
      containers.foreach { cc =>
        if (s.isIntegration(cc.getPlayerInstance())) {
          if (cc.isConstructed() && sync == null) {
            sync = s.getNewInstance()
          }
          if (sync != null) {
            cc.getManagerInstance() play sync.getNextIntegrationRole(cc.getPlayerInstance())
          }
        }
      }
      if (sync != null)
        SynchronizationCompartment combine sync
    }
  }

  /**
   * Fill the test lists with all Players from the ConstructionContainers.
   */
  protected def fillTestLists(containers: ListBuffer[ConstructionContainer]): Unit = {
    containers.foreach { cc =>
      ModelElementLists.addElement(cc.getPlayerInstance())
    }
  }
  
  protected def makeCompleteConstructionProcess(containers: ListBuffer[ConstructionContainer]): Unit = {
    this.addManagerRoles(containers)
    this.addDeleteRoles(containers)
    this.addRelatedRoleManager(containers)
    this.synchronizeCompartments(containers)
    this.bindSynchronizationRules(containers)
    this.fillTestLists(containers)    
  }
}