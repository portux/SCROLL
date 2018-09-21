package scroll.internal.ecore

import org.eclipse.emf.common.util.URI
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl

/**
  * Trait providing functionality for importing ecore models.
  * Remember to set the <code>path</code> variable!
  */
trait ECoreImporter {
  var path: String = _

  /*private val META_MODEL_PATH = getClass.getResource("/crom_l1_composed.ecore").getPath

  private def registerMetaModel(rs: ResourceSetImpl): Unit = {
    Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap.put(
      "ecore", new EcoreResourceFactoryImpl())

    val extendedMetaData = new BasicExtendedMetaData(rs.getPackageRegistry)
    rs.getLoadOptions.put(XMLResource.OPTION_EXTENDED_META_DATA, extendedMetaData)

    val r = rs.getResource(URI.createFileURI(META_MODEL_PATH), true)
    val eObject = r.getContents.get(0)
    eObject match {
      case p: EPackage => val _ = rs.getPackageRegistry.put(p.getNsURI, p)
      case _ => throw new IllegalStateException("Meta-Model for CROM could not be loaded!")
    }
  }*/

  /**
    * Load and imports an ecore model.
    * Remember to set the <code>path</code> variable!
    *
    * @return the imported model as Resource
    */
  protected def loadModel(): Resource = {
    require(null != path && path.nonEmpty)

    val resourceSet = new ResourceSetImpl()
    //registerMetaModel(resourceSet)
    resourceSet.getResourceFactoryRegistry.getExtensionToFactoryMap.put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl())
    val r = resourceSet.getResource(URI.createFileURI(path), true)

    require(null != r)
    require(!r.getContents.isEmpty)
    r
  }
}
