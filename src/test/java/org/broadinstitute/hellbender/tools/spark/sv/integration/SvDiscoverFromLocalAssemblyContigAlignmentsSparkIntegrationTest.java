package org.broadinstitute.hellbender.tools.spark.sv.integration;

import org.apache.hadoop.fs.Path;
import org.broadinstitute.hellbender.CommandLineProgramTest;
import org.broadinstitute.hellbender.GATKBaseTest;
import org.broadinstitute.hellbender.utils.test.ArgumentsBuilder;
import org.broadinstitute.hellbender.utils.test.MiniClusterUtils;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SvDiscoverFromLocalAssemblyContigAlignmentsSparkIntegrationTest extends CommandLineProgramTest {

    private static final class TestArgs {
        final String outputDir;
        final String cnvCallsLoc;

        TestArgs(final String outputDir, final String cnvCallsLoc){
            this.outputDir = outputDir;
            this.cnvCallsLoc = cnvCallsLoc;
        }

        String getCommandLine() {
            return  " -R " + SVIntegrationTestDataProvider.reference_2bit +
                    " -I " + SVIntegrationTestDataProvider.TEST_CONTIG_SAM +
                    " -O " + outputDir + "/SvDiscoverFromLocalAssemblyContigAlignmentsSparkIntegrationTest" +
                    (cnvCallsLoc == null ? "" : " --cnv-calls " + cnvCallsLoc);
        }
    }


    @DataProvider(name = "svDiscoverFromLocalAssemblyContigAlignmentsSparkIntegrationTest")
    public Object[][] createTestData() {
        List<Object[]> tests = new ArrayList<>();
        final File tempDirLeft = GATKBaseTest.createTempDir("forLeft");
        tempDirLeft.deleteOnExit();
        tests.add(new Object[]{new TestArgs(tempDirLeft.getAbsolutePath(), SVIntegrationTestDataProvider.EXTERNAL_CNV_CALLS)});
        return tests.toArray(new Object[][]{});
    }

    @Test(dataProvider = "svDiscoverFromLocalAssemblyContigAlignmentsSparkIntegrationTest", groups = "sv")
    public void testDiscoverVariantsRunnableLocal(final TestArgs params) throws Exception {

        final List<String> args = Arrays.asList( new ArgumentsBuilder().add(params.getCommandLine()).getArgsArray() );
        runCommandLine(args);
        final String newVCF = args.get(args.indexOf("-O") + 1) + "_sample_NonComplex.vcf";
        StructuralVariationDiscoveryPipelineSparkIntegrationTest.svDiscoveryVCFEquivalenceTest(newVCF,
                SVIntegrationTestDataProvider.EXPECTED_SIMPLE_DEL_VCF, null,
                DiscoverVariantsFromContigAlignmentsSAMSparkIntegrationTest.annotationsToIgnoreWhenComparingVariants,
                false);
    }

    @Test(dataProvider = "svDiscoverFromLocalAssemblyContigAlignmentsSparkIntegrationTest", groups = "sv")
    public void testDiscoverVariantsRunnableMiniCluster(final TestArgs params) throws Exception {

        MiniClusterUtils.runOnIsolatedMiniCluster(cluster -> {

            final List<String> argsToBeModified = Arrays.asList( new ArgumentsBuilder().add(params.getCommandLine()).getArgsArray() );
            final Path workingDirectory = MiniClusterUtils.getWorkingDir(cluster);

            int idx = 0;

            idx = argsToBeModified.indexOf("-I");
            Path path = new Path(workingDirectory, "hdfs.sam");
            File file = new File(argsToBeModified.get(idx+1));
            cluster.getFileSystem().copyFromLocalFile(new Path(file.toURI()), path);
            argsToBeModified.set(idx+1, path.toUri().toString());

            idx = argsToBeModified.indexOf("-R");
            path = new Path(workingDirectory, "reference.2bit");
            file = new File(argsToBeModified.get(idx+1));
            cluster.getFileSystem().copyFromLocalFile(new Path(file.toURI()), path);
            argsToBeModified.set(idx+1, path.toUri().toString());

            // outputs, prefix with hdfs address
            idx = argsToBeModified.indexOf("-O");
            path = new Path(workingDirectory, "test");
            final String vcfOnHDFS = path.toUri().toString() + "_sample_NonComplex.vcf";
            argsToBeModified.set(idx+1, path.toUri().toString());

            runCommandLine(argsToBeModified);
            StructuralVariationDiscoveryPipelineSparkIntegrationTest.svDiscoveryVCFEquivalenceTest(
                    vcfOnHDFS,
                    SVIntegrationTestDataProvider.EXPECTED_SIMPLE_DEL_VCF,
                    null,
                    DiscoverVariantsFromContigAlignmentsSAMSparkIntegrationTest.annotationsToIgnoreWhenComparingVariants,
                    true);
        });
    }
}
