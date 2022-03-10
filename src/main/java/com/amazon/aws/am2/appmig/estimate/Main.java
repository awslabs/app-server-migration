package com.amazon.aws.am2.appmig.estimate;

import com.amazon.aws.am2.appmig.glassviewer.db.AppDiscoveryGraphDB;
import com.amazon.aws.am2.appmig.glassviewer.db.IAppDiscoveryGraphDB;

/**
 * This class is the starting point of the Application Migration Factory tool.
 * It takes 2 arguments. The first argument is the path of the source folder
 * which needs to be migrated and the second argument is the path of the file
 * where the migration report needs to be generated
 *
 * @author agoteti
 */
public class Main {

    public static void main(String[] args) throws Exception {
        if (args != null && args.length == 4) {
            String source = args[0];
            String target = args[1];
            String user = args[2];
            String password = args[3];

            AppDiscoveryGraphDB.setConnectionProperties(user, password);
            Estimator estimator = ProjectEstimator.getEstimator(source);
            estimator.build(source, target);

            IAppDiscoveryGraphDB db = AppDiscoveryGraphDB.getInstance();
            db.close();
        }
    }
}
