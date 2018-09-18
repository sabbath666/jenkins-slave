@Grab('org.yaml:snakeyaml:1.23')
import org.yaml.snakeyaml.Yaml

import java.nio.file.Files
import java.nio.file.Paths
import java.text.SimpleDateFormat
import groovy.sql.Sql
import com.informix.jdbc.IfxDriver

Yaml parser = new Yaml()
def sf = new SimpleDateFormat("dd.MM.yyyy HH:mm:SSS")
def config = parser.loadAs(("src/upgrade.yml" as File).text, Map)
def version = config.upgrade.version
def upgradeScript = "./scr$version" as File
if (upgradeScript.isFile()) upgradeScript.delete()
upgradeScript << "# UPGRADE $version SHARE\n"
upgradeScript << "## DATE : ${sf.format(new Date())}\n"
upgradeScript << "################ TARGET SYSTEMS ################\n"
config.upgrade.target.each {
    upgradeScript << "#ADDRESS \$LIST_DBNAME ${it.key}  ${it.value}\n"
}
upgradeScript << "#################### FILES #####################\n"
File files = 'changed_files.chg' as File

if (!files.text.contains('upgrade.yml')) throw new RuntimeException("Please, execute upgrade.groovy!!")

files.eachLine {
    if (!line.contains('upgrade.yml'))
        upgradeScript << "makesql \$LIST_DBNAME $it\n"
}

def upgrDir = new File("upgrade")
upgrDir.mkdirs()
files.readLines().each {
    def file = new File(it)
    try {
        if (!line.contains('upgrade.yml'))
            Files.copy(Paths.get(file.canonicalPath), Paths.get(new File(upgrDir.canonicalPath, file.name).canonicalPath))
    }
    catch (Exception ex) {
    }
}

Files.copy(Paths.get(upgradeScript.canonicalPath), Paths.get(new File(upgrDir.canonicalPath, upgradeScript.name).canonicalPath))

"tar -C ${upgrDir.canonicalPath} -cvf upgr${version}.tar .".execute()
"scp upgr${version}.tar root@45.55.43.205:/root/users/sabbath".execute()

def sql = Sql.newInstance('jdbc:informix-sqli://45.55.43.205:9088/mydb:INFORMIXSERVER=informix', 'sabbath', 'sabbath', 'com.informix.jdbc.IfxDriver')
def upgradeName = "upgr${version}.tar"
sql.execute "insert into updates(filename) values ('$upgradeName')"

println "=============> OK <================"