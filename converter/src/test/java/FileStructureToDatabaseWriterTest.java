import com.astrodoorways.db.filesystem.BaseFileStructureToDatabaseWriter;
import com.astrodoorways.db.filesystem.FileInfo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class FileStructureToDatabaseWriterTest {

	@Test
	public void test() {
		FileInfo dto = new FileInfo();
		dto.setFileName("name");
		dto.setDirectory("dir");
		dto.setExtension("ext");
	}

	@Test
	public void filetoDatabaseTest() throws IOException {
		BaseFileStructureToDatabaseWriter writer = new BaseFileStructureToDatabaseWriter();
		writer.writeFileStructure(new File("src/test/resources/test-dirs/read/data/cassini"));
		assertTrue(writer.getCollectionOfFiles().size() > 0);
	}
}
