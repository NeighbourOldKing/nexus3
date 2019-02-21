import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.storage.Query
import org.sonatype.nexus.repository.storage.StorageFacet
import org.sonatype.nexus.repository.storage.Asset
import org.sonatype.nexus.repository.storage.Component
def repositoryName = "npm-snapshot"
def versions = []
def temp = []
/**
 * 删除npm-snapshot库中较低版本的npm类型的component和asset数据
 * 只保留npm类型最新的snapshots版本(不对release和其它版本做操作)
 */
repository.repositoryManager.browse().each { Repository repo ->
	def tx = repo.facet(StorageFacet).txSupplier().get()
	try {
		tx.begin()
		if(repo.name.indexOf(repositoryName)!= -1){
			Iterable<Component> allcomponets = tx.findComponents(Query.builder().build(), [repo])
			for(def tempcomponent in allcomponets){
				temp.add( tempcomponent.name())
			}

			HashSet hashSet = new  HashSet(temp);
			temp.clear();
			temp.addAll(hashSet);

			for(int i = 0;i< temp.size(); i++) {
				Iterable<Component> components = tx.findComponents(Query.builder().where(" name ").eq(temp[i]).build(), [repo])
				versions = components.collect{ "${it.version()}"}
				def needRemoveVersion = [];
				def lastSnapshottVersion = versions[versions.size()-1]
				for(int j = 0 ;j < versions.size() ;j++ ){
					if(versions[j].indexOf("-SNAPSHOT") !=-1 && versions[j] != lastSnapshottVersion){
						needRemoveVersion.add(versions[j])
					}
				}

				log.info("----组件名称-----" + temp[i]);
				log.info("----对应最新开发版本---------" + lastSnapshottVersion);
				log.info("----需要删除删除的对应版本---------" + needRemoveVersion);
				log.info("------结束！！！！！--------");

				if(needRemoveVersion.size() > 0 ){
					log.info("-长度！--" + needRemoveVersion.size() );
					for(int k = 0 ; k< needRemoveVersion.size() ; k++){
						Iterable<Component> needDeleteComponents = tx.findComponents(Query.builder().where(" name = '" +temp[i] + "' and version = '" + needRemoveVersion[k]+ "'").build(), [repo])
						Iterator it = 	needDeleteComponents.iterator();
						while(it.hasNext()){
							tx.deleteComponent(it.next());
						}
					}

				}

			}
		}

		tx.commit()
	} catch (Exception e) {
		log.warn("Transaction failed {}", e.toString())
		tx.rollback()
	} finally {
		tx.close()
	}
}



