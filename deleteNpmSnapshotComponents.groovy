import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.storage.Query
import org.sonatype.nexus.repository.storage.StorageFacet
import org.sonatype.nexus.repository.storage.Component
def repositoryName = "npm-snapshot"
def versions = []
def temp = []

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

				log.info("----开始-----" + temp[i]);
				log.info("----最新版本---------" + lastSnapshottVersion);
				log.info("----需要删除的版本---------" + needRemoveVersion);
				log.info("-----结束--------");

				if(needRemoveVersion.size() > 0 ){
					log.info("-需要删除的个数：--" + needRemoveVersion.size() );
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



