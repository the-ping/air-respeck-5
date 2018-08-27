package com.specknet.airrespeck.models;

public class SVC {

    private enum Kernel {LINEAR, POLY, RBF, SIGMOID}

    private int nClasses;
    private int nRows;
    private int[] classes;
    private double[][] vectors;
    private double[][] coefficients;
    private double[] intercepts;
    private int[] weights;
    private Kernel kernel;
    private double gamma;
    private double coef0;
    private double degree;

    public SVC(int nClasses, int nRows, double[][] vectors, double[][] coefficients, double[] intercepts, int[] weights, String kernel, double gamma, double coef0, double degree) {
        this.nClasses = nClasses;
        this.classes = new int[nClasses];
        for (int i = 0; i < nClasses; i++) {
            this.classes[i] = i;
        }
        this.nRows = nRows;

        this.vectors = vectors;
        this.coefficients = coefficients;
        this.intercepts = intercepts;
        this.weights = weights;

        this.kernel = Kernel.valueOf(kernel.toUpperCase());
        this.gamma = gamma;
        this.coef0 = coef0;
        this.degree = degree;
    }

    public int predict(double[] features) {

        double[] kernels = new double[vectors.length];
        double kernel;
        switch (this.kernel) {
            case LINEAR:
                // <x,x'>
                for (int i = 0; i < this.vectors.length; i++) {
                    kernel = 0.;
                    for (int j = 0; j < this.vectors[i].length; j++) {
                        kernel += this.vectors[i][j] * features[j];
                    }
                    kernels[i] = kernel;
                }
                break;
            case POLY:
                // (y<x,x'>+r)^d
                for (int i = 0; i < this.vectors.length; i++) {
                    kernel = 0.;
                    for (int j = 0; j < this.vectors[i].length; j++) {
                        kernel += this.vectors[i][j] * features[j];
                    }
                    kernels[i] = Math.pow((this.gamma * kernel) + this.coef0, this.degree);
                }
                break;
            case RBF:
                // exp(-y|x-x'|^2)
                for (int i = 0; i < this.vectors.length; i++) {
                    kernel = 0.;
                    for (int j = 0; j < this.vectors[i].length; j++) {
                        kernel += Math.pow(this.vectors[i][j] - features[j], 2);
                    }
                    kernels[i] = Math.exp(-this.gamma * kernel);
                }
                break;
            case SIGMOID:
                // tanh(y<x,x'>+r)
                for (int i = 0; i < this.vectors.length; i++) {
                    kernel = 0.;
                    for (int j = 0; j < this.vectors[i].length; j++) {
                        kernel += this.vectors[i][j] * features[j];
                    }
                    kernels[i] = Math.tanh((this.gamma * kernel) + this.coef0);
                }
                break;
        }

        int[] starts = new int[this.nRows];
        for (int i = 0; i < this.nRows; i++) {
            if (i != 0) {
                int start = 0;
                for (int j = 0; j < i; j++) {
                    start += this.weights[j];
                }
                starts[i] = start;
            } else {
                starts[0] = 0;
            }
        }

        int[] ends = new int[this.nRows];
        for (int i = 0; i < this.nRows; i++) {
            ends[i] = this.weights[i] + starts[i];
        }

        if (this.nClasses == 2) {

            for (int i = 0; i < kernels.length; i++) {
                kernels[i] = -kernels[i];
            }

            double decision = 0.;
            for (int k = starts[1]; k < ends[1]; k++) {
                decision += kernels[k] * this.coefficients[0][k];
            }
            for (int k = starts[0]; k < ends[0]; k++) {
                decision += kernels[k] * this.coefficients[0][k];
            }
            decision += this.intercepts[0];

            if (decision > 0) {
                return 0;
            }
            return 1;

        }

        double[] decisions = new double[this.intercepts.length];
        for (int i = 0, d = 0, l = this.nRows; i < l; i++) {
            for (int j = i + 1; j < l; j++) {
                double tmp = 0.;
                for (int k = starts[j]; k < ends[j]; k++) {
                    tmp += this.coefficients[i][k] * kernels[k];
                }
                for (int k = starts[i]; k < ends[i]; k++) {
                    tmp += this.coefficients[j - 1][k] * kernels[k];
                }
                decisions[d] = tmp + this.intercepts[d];
                d++;
            }
        }

        int[] votes = new int[this.intercepts.length];
        for (int i = 0, d = 0, l = this.nRows; i < l; i++) {
            for (int j = i + 1; j < l; j++) {
                votes[d] = decisions[d] > 0 ? i : j;
                d++;
            }
        }

        int[] amounts = new int[this.nClasses];
        for (int i = 0, l = votes.length; i < l; i++) {
            amounts[votes[i]] += 1;
        }

        int classVal = -1, classIdx = -1;
        for (int i = 0, l = amounts.length; i < l; i++) {
            if (amounts[i] > classVal) {
                classVal = amounts[i];
                classIdx = i;
            }
        }
        return this.classes[classIdx];

    }

    public static void main(String[] args) {
        if (args.length == 6) {

            // Features:
            double[] features = new double[args.length];
            for (int i = 0, l = args.length; i < l; i++) {
                features[i] = Double.parseDouble(args[i]);
            }

            // Parameters:
            double[][] vectors = {{-0.0729733871071, -0.373053001356, -0.475758930612, 1.57599610165, -0.588849021219, -0.149858817182}, {0.00741652176521, -0.373053001356, -0.462498571049, 1.45637077, -0.563196700238, -2.36173554221}, {-0.247993431038, -0.622781989188, -0.462969189546, 0.379742785151, -0.568009301634, -1.03460950719}, {0.234559667411, -0.373053001356, -0.485124879303, 1.45637077, -0.571430777128, -0.592234162187}, {1.08720023944, -0.872510977019, -0.456137755183, 0.140492121852, -0.554104398823, 0.513704200326}, {0.0762568667842, -0.622781989188, -0.465982070569, -0.936135862994, -0.557571184681, 0.734891872828}, {-0.213656057142, -1.37196895268, -0.474057258297, -0.0987585414474, -0.573671053975, 2.06201790784}, {0.228965350292, -0.123324013525, -0.331665729784, 0.4993681168, -0.481498880599, 0.0713288553204}, {-0.972553157739, 1.12532092563, -0.476586810988, 0.738618780099, -0.612086282399, -1.6981725247}, {-0.927244604514, 1.6247789013, -0.44526831757, -0.696885199695, -0.613320065065, 0.956079545331}, {-0.887300724296, -1.37196895268, -0.485342156758, -1.41463718959, -0.595461223942, 0.513704200326}, {0.00541066147169, -0.622781989188, -0.369716916696, 2.05449742824, -0.439798829613, 1.17726721783}, {0.252994018049, -0.622781989188, -0.39313485042, 0.260117453501, -0.480238663381, -0.371046489685}, {-0.612935455463, -1.12223996485, -0.393227590653, 0.140492121852, -0.544681194789, -1.03460950719}, {-0.537771260039, 0.126404974307, -0.486837895859, 1.09749477505, -0.602482076007, 0.0713288553204}, {-0.0989689431263, -1.37196895268, -0.392483415286, 0.379742785151, -0.492619382053, -0.81342183469}, {-0.0762585803278, -1.37196895268, -0.409134470125, 0.61899344845, -0.470940465924, -0.149858817182}, {-0.49894869131, -1.37196895268, -0.437798000774, -0.218383873097, -0.502958515872, -0.149858817182}, {-0.338387496343, -0.622781989188, -0.462126954381, 0.4993681168, -0.521124537657, 0.292516527823}, {-0.640806608641, -0.373053001356, -0.452867187369, -0.457634536396, -0.539349880348, 0.513704200326}, {-0.738768365161, -1.12223996485, -0.426593142505, 0.0208667902021, -0.546792270442, -0.81342183469}, {1.68943304053, 0.875591937801, 0.1812628606, 0.61899344845, 0.121287124562, 1.84083023534}, {1.65027784773, 0.875591937801, 0.0448132757443, 0.858244111749, 0.0799594506295, -1.03460950719}, {1.14419678978, 1.12532092563, 0.379616624121, -1.41463718959, 0.59139890494, -1.03460950719}, {1.05584244461, 3.12315282828, 0.853140870949, -0.218383873097, 1.19800020714, 0.513704200326}, {0.992363207736, 0.376133962138, 0.780477077222, -2.37163984279, 1.65980539709, -0.81342183469}, {0.901963339362, 1.37504991346, 1.22225559498, 0.379742785151, 1.90756236749, -0.371046489685}, {0.56954301257, 0.376133962138, 1.51331343454, -2.37163984279, 1.15415069881, 1.17726721783}, {0.876211623885, 1.12532092563, 1.48256456768, -1.41463718959, 1.29718722434, 0.292516527823}, {1.52597838768, 0.62586294997, 0.766453842651, -2.25201451114, 0.783083127125, 2.72558092535}, {1.39988984669, 0.875591937801, 1.27444505517, 0.0208667902021, 1.89087475363, -0.81342183469}, {1.42993148013, 1.12532092563, 0.912839888524, -1.05576119464, 0.93261783381, 2.06201790784}, {-0.613607991851, 0.62586294997, -0.487672196962, 0.4993681168, -0.620810962221, 0.956079545331}, {0.714330556535, -1.12223996485, -0.428233344908, 0.738618780099, -0.548014069415, 0.956079545331}, {2.51271373692, -1.37196895268, -0.276585146627, -0.457634536396, -0.524632420984, 0.0713288553204}, {5.50077331788, -1.37196895268, -0.270618406249, -0.936135862994, -0.472503331099, -0.371046489685}, {1.6721011442, -0.872510977019, -0.27454951844, 0.0208667902021, -0.47431891212, 0.292516527823}, {-0.208527124006, -0.622781989188, -0.475252707141, 0.4993681168, -0.591120393826, 0.292516527823}, {-0.276452426087, -0.373053001356, -0.483053108868, -0.0987585414474, -0.60151157206, -1.2557971797}, {-0.495551059922, 0.62586294997, -0.490501791171, 0.0208667902021, -0.618105861427, 0.956079545331}, {-0.388616071132, 0.62586294997, -0.491115605217, -0.457634536396, -0.613029955941, 0.734891872828}, {-0.40783976306, 0.126404974307, -0.496626180304, 0.0208667902021, -0.600367013305, 0.292516527823}, {-0.418314053, 0.376133962138, -0.490349082323, 0.140492121852, -0.593503500052, -0.592234162187}, {-0.532465469262, 0.62586294997, -0.497937795993, 1.09749477505, -0.611197372522, -1.2557971797}, {-0.551751717282, 0.62586294997, -0.499911973859, 0.0208667902021, -0.603585407585, -0.592234162187}, {-0.504262363096, -0.123324013525, -0.478793159881, 0.0208667902021, -0.626713392194, -1.4769848522}, {-0.484849624407, 0.62586294997, -0.476578631084, 0.140492121852, -0.634070894169, 2.06201790784}, {-0.499073168314, 0.62586294997, -0.501725317124, -0.218383873097, -0.624869081436, 0.956079545331}, {-0.500465317683, 0.376133962138, -0.490508913488, 0.738618780099, -0.599482857888, 1.61964256284}, {-0.577030433033, 0.62586294997, -0.499419310025, 0.738618780099, -0.599585337424, 1.17726721783}, {-0.66317063987, 0.62586294997, -0.508505359672, 1.6956214333, -0.593424473105, -0.149858817182}, {0.0387459199307, -1.12223996485, -0.448417973717, 1.33674543835, -0.568448209781, 1.39845489034}, {-0.432758655142, -0.373053001356, -0.447295808361, -0.577259868045, -0.568917752868, 0.292516527823}, {-0.615262246923, 0.126404974307, -0.467656679075, -0.577259868045, -0.572697749569, -0.81342183469}, {1.34925892712, -1.12223996485, -0.32720972549, -1.29501185794, -0.41276523625, 0.513704200326}, {0.879781903519, -0.622781989188, -0.292399955936, -0.457634536396, -0.305096800837, 1.61964256284}, {0.631271129347, -0.872510977019, -0.313387921674, 0.858244111749, -0.30374470079, 0.292516527823}, {-0.00868997702658, -0.872510977019, -0.457742110491, -1.17538652629, -0.59271007403, 0.734891872828}, {-0.613017183687, -1.37196895268, -0.477084146364, -1.41463718959, -0.571793572407, 0.0713288553204}, {-1.08672422213, 2.12423687696, -0.498526788102, 0.4993681168, -0.613523901933, -1.2557971797}, {2.28511581152, 1.6247789013, 0.243779492272, -1.29501185794, 0.636965701724, -1.4769848522}, {2.65692966259, 1.6247789013, 0.409466323749, -0.577259868045, 0.648767013188, -1.4769848522}, {1.98503557408, 0.875591937801, 0.802899931193, 0.61899344845, 0.694683939321, 1.17726721783}, {2.38335518455, 1.12532092563, 0.48184261434, 0.4993681168, 0.479416387022, -2.14054786971}, {2.52083158272, 0.62586294997, 0.225059277961, 0.61899344845, 0.45330929431, 0.956079545331}, {2.6870667924, 0.62586294997, 0.239614070421, 1.45637077, 0.613429788649, -0.81342183469}, {2.57962668071, 1.6247789013, 0.213003478332, 0.379742785151, 0.880109984998, -0.149858817182}, {1.69667039969, 1.6247789013, 0.130747654411, 0.0208667902021, 0.671305317719, -1.2557971797}, {2.14422769061, 1.6247789013, 0.210932749349, 1.6956214333, 0.785282225689, -1.03460950719}, {1.78876744659, 1.6247789013, 0.399052352629, 0.260117453501, 0.490063468315, -0.592234162187}, {2.14164684439, 2.12423687696, 0.219519078571, -0.0987585414474, 0.64274893587, 1.84083023534}, {1.55266526031, 0.62586294997, 0.446202473053, -0.218383873097, 0.677878797709, 0.292516527823}, {-0.932186148264, 1.6247789013, -0.504149847959, 0.738618780099, -0.597111074042, -0.371046489685}, {0.728918365639, 0.126404974307, 1.00873997287, -2.37163984279, 2.01186036655, -1.03460950719}, {0.914612561232, 2.37396586479, 0.842726904899, -2.37163984279, 1.23867660376, -2.36173554221}, {0.842753087775, 0.62586294997, 1.1218441116, -1.29501185794, 2.23478370044, -0.371046489685}, {0.728918365639, 0.126404974307, 1.00873997287, -2.37163984279, 2.01186036655, -1.03460950719}, {0.762798535913, 2.37396586479, 0.660355957834, -1.77351318454, 1.1616982097, 0.292516527823}, {0.198494124944, 2.37396586479, 0.754243775006, -1.05576119464, 0.654155100109, 0.0713288553204}, {-0.407498791522, -0.872510977019, -0.475948359435, -0.218383873097, -0.545093607949, 0.956079545331}, {-0.284071550264, 0.126404974307, -0.489196854976, -0.936135862994, -0.602299764131, 0.513704200326}, {-0.403864768853, 0.126404974307, -0.490201139657, -1.41463718959, -0.600385909254, 2.50439325285}, {-0.528356091213, 0.126404974307, -0.477374997177, -1.17538652629, -0.567125211871, -1.03460950719}, {-0.5048815028, 1.12532092563, -0.478414449012, -1.29501185794, -0.577525881272, -1.9193601972}, {-0.48615063671, -0.123324013525, -0.488843827338, 0.0208667902021, -0.595261536118, 0.734891872828}, {-0.593893352237, 0.62586294997, -0.503859557057, 0.260117453501, -0.584552838307, -0.592234162187}, {-0.342656830814, 0.376133962138, -0.498766707121, 0.379742785151, -0.606896785943, -0.149858817182}, {-0.506899493563, 0.62586294997, -0.495487122625, 0.858244111749, -0.616240715999, -1.2557971797}, {-0.569054962285, 0.62586294997, -0.485023934752, -1.05576119464, -0.618009446515, 1.84083023534}, {-0.536585612819, 0.62586294997, -0.491026807072, 0.260117453501, -0.619958886485, -1.2557971797}, {-0.374734343286, -0.622781989188, -0.487724190457, -0.696885199695, -0.56927157625, 1.17726721783}, {-0.402842105964, 0.126404974307, -0.479844996341, -0.0987585414474, -0.588958858711, 0.0713288553204}, {-0.393619155224, 0.126404974307, -0.483511142171, 0.0208667902021, -0.604001307345, -0.149858817182}, {-0.469454486881, 0.126404974307, -0.490839210253, -2.01276384784, -0.585864296382, 0.956079545331}, {-0.619111692083, 0.875591937801, -0.500126336928, -1.89313851619, -0.580056371516, 0.292516527823}, {-0.530762546216, 0.62586294997, -0.498927746535, -2.37163984279, -0.619929520602, -0.371046489685}, {-0.679839795309, 0.126404974307, -0.500038369557, -0.696885199695, -0.5975440294, -1.9193601972}, {-0.414206668092, -0.622781989188, -0.499979191987, -1.17538652629, -0.570167493659, 0.0713288553204}, {-0.512201350097, -0.622781989188, -0.484857898865, -1.77351318454, -0.572178606004, -1.03460950719}, {0.627333139086, 1.37504991346, 5.37601199638, -1.65388785289, 3.01432993587, 2.06201790784}, {1.19336567727, 0.875591937801, 5.33264877036, -0.457634536396, 3.78272931832, 2.06201790784}, {-0.887300724296, -1.37196895268, -0.485342156758, -1.41463718959, -0.595461223942, 0.513704200326}, {0.00541066147169, -0.622781989188, -0.369716916696, 2.05449742824, -0.439798829613, 1.17726721783}, {0.252994018049, -0.622781989188, -0.39313485042, 0.260117453501, -0.480238663381, -0.371046489685}, {-0.612935455463, -1.12223996485, -0.393227590653, 0.140492121852, -0.544681194789, -1.03460950719}, {-0.537771260039, 0.126404974307, -0.486837895859, 1.09749477505, -0.602482076007, 0.0713288553204}, {0.228965350292, -0.123324013525, -0.331665729784, 0.4993681168, -0.481498880599, 0.0713288553204}, {0.00615987726847, -0.123324013525, -0.455999691822, 0.140492121852, -0.576777669924, -1.4769848522}, {1.41891135474, -1.37196895268, -0.457647026755, 0.260117453501, -0.583417835182, 1.39845489034}, {0.200137376578, -1.37196895268, -0.410387007579, -1.05576119464, -0.540731126837, 0.956079545331}, {-0.182832577347, 0.376133962138, -0.483752641123, 1.45637077, -0.594647813881, 1.17726721783}, {0.5121500471, -0.373053001356, -0.489770612189, 1.2171201067, -0.609947293907, -0.81342183469}, {-0.370019983009, -0.373053001356, -0.457850980109, -1.77351318454, -0.592550761117, 1.84083023534}, {0.0762568667842, -0.622781989188, -0.465982070569, -0.936135862994, -0.557571184681, 0.734891872828}, {-0.213656057142, -1.37196895268, -0.474057258297, -0.0987585414474, -0.573671053975, 2.06201790784}, {0.569238161891, -1.12223996485, -0.372429981464, 0.0208667902021, -0.366589241549, -1.9193601972}, {1.02589506202, -1.37196895268, -0.410108447342, 0.140492121852, -0.378006044205, -1.03460950719}, {0.163907077054, -0.872510977019, -0.447428255942, -0.816510531344, -0.55779589911, -1.03460950719}, {0.589435397961, -0.872510977019, -0.437903535987, 0.4993681168, -0.599528279564, -0.371046489685}, {1.54302405582, -1.37196895268, -0.433857546309, -1.65388785289, -0.559469612082, -0.592234162187}, {2.39546146338, -1.37196895268, -0.476163348947, -0.218383873097, -0.560525149806, -0.81342183469}, {1.06024110648, -0.872510977019, -0.434891357805, -1.77351318454, -0.547875605567, -0.81342183469}, {1.56476849933, -1.37196895268, -0.443882728136, 1.45637077, -0.549163064744, -0.371046489685}, {0.265302005863, -0.622781989188, -0.486834515984, 0.858244111749, -0.567261627335, -0.149858817182}, {0.606489092443, -0.872510977019, -0.479444787988, -1.17538652629, -0.599720754547, 0.734891872828}, {-0.0192861691532, -0.123324013525, -0.393756163857, 0.0208667902021, -0.514882891419, -1.9193601972}, {0.110092082111, -0.622781989188, -0.444266148105, -0.936135862994, -0.600171796349, -1.9193601972}, {-0.0202571753589, -0.872510977019, -0.460004854892, 0.0208667902021, -0.535221563867, 0.734891872828}, {-0.0066723352712, -0.872510977019, -0.490982152474, -1.05576119464, -0.549483559296, -1.03460950719}, {0.0781421637446, -0.872510977019, -0.489754156562, 0.140492121852, -0.550068853377, -1.6981725247}, {0.234559667411, -0.373053001356, -0.485124879303, 1.45637077, -0.571430777128, -0.592234162187}, {1.08720023944, -0.872510977019, -0.456137755183, 0.140492121852, -0.554104398823, 0.513704200326}, {1.04612750308, -1.37196895268, -0.471630022138, 0.260117453501, -0.538072098224, -1.6981725247}, {-0.0729733871071, -0.373053001356, -0.475758930612, 1.57599610165, -0.588849021219, -0.149858817182}, {0.00741652176521, -0.373053001356, -0.462498571049, 1.45637077, -0.563196700238, -2.36173554221}, {-0.247993431038, -0.622781989188, -0.462969189546, 0.379742785151, -0.568009301634, -1.03460950719}, {0.979248145897, 0.875591937801, 5.378076256, -2.01276384784, 5.27257053141, 0.513704200326}, {1.77972139267, 0.376133962138, 5.18389109229, -0.936135862994, 5.67973109832, 0.513704200326}, {2.00907277432, -0.622781989188, 4.96543835191, -0.936135862994, 4.27054271283, -0.81342183469}, {1.81589278131, -0.872510977019, 4.98121000619, -0.936135862994, 4.67955525238, 0.292516527823}, {0.932171219754, 2.12423687696, 5.066656096, -0.936135862994, 4.75081356899, -0.81342183469}, {-0.971266136811, -1.37196895268, -0.491823018565, -1.53426252124, -0.592962301639, -0.149858817182}, {-0.938904323864, -1.37196895268, -0.489773027158, -0.936135862994, -0.586254841755, -0.81342183469}, {-0.231767928347, -1.37196895268, -0.439284316019, -0.457634536396, -0.52427734035, 1.17726721783}, {-0.238526095734, -1.12223996485, -0.428682433213, 0.858244111749, -0.521709971984, 0.292516527823}, {-0.876132722691, -1.12223996485, -0.442912588258, 0.0208667902021, -0.552390664914, 0.292516527823}, {-1.09268062165, 1.87450788913, -0.48901755995, 1.45637077, -0.610982299538, 1.84083023534}, {-0.748393433399, 0.875591937801, -0.470396263029, 2.17412275989, -0.60887648018, 0.292516527823}, {-0.818650773016, 1.6247789013, -0.489084460719, -0.816510531344, -0.592811082155, 2.06201790784}, {-0.809167159552, 1.12532092563, -0.490540837205, -0.816510531344, -0.599847854055, -1.6981725247}, {-0.564824879445, -1.37196895268, -0.487007114911, -1.53426252124, -0.52475982229, -1.6981725247}, {-0.985574245959, 1.6247789013, -0.505990798156, 1.2171201067, -0.599891360588, 1.84083023534}, {-0.932186148264, 1.6247789013, -0.504149847959, 0.738618780099, -0.597111074042, -0.371046489685}, {-0.960989353127, 1.37504991346, -0.462026805446, 0.4993681168, -0.561382733174, -1.03460950719}, {-0.613607991851, 0.62586294997, -0.487672196962, 0.4993681168, -0.620810962221, 0.956079545331}, {-0.66317063987, 0.62586294997, -0.508505359672, 1.6956214333, -0.593424473105, -0.149858817182}, {-0.00868997702658, -0.872510977019, -0.457742110491, -1.17538652629, -0.59271007403, 0.734891872828}, {-0.751597144898, -0.622781989188, -0.487609002465, 1.2171201067, -0.580874344267, 2.06201790784}, {-0.593361107505, 0.126404974307, -0.484502012535, 0.4993681168, -0.56059766095, 0.0713288553204}, {-0.786928061686, -0.123324013525, -0.474519506545, 0.738618780099, -0.600519401763, 2.06201790784}, {-0.729042128955, 0.376133962138, -0.480687715379, 0.61899344845, -0.608296801849, 2.06201790784}, {-0.638807259991, -0.622781989188, -0.50499313852, 0.379742785151, -0.612534518397, 2.50439325285}, {-0.381989373444, 0.126404974307, -0.497531199281, 0.379742785151, -0.617888137941, 0.292516527823}, {-0.368053270829, -0.123324013525, -0.507973184571, 0.140492121852, -0.61438962465, -0.592234162187}, {0.89453445805, 1.12532092563, 1.38179010867, -0.0987585414474, 1.11308415741, -0.81342183469}, {0.817740960228, 2.37396586479, 0.947433657258, 1.93487209659, 1.23170103432, -0.81342183469}, {0.380122209307, 0.62586294997, 0.59252610636, -1.41463718959, 1.61366051111, -2.14054786971}, {1.04543859989, 0.376133962138, 0.895573872702, -1.17538652629, 1.80441635537, -2.36173554221}, {0.533616565117, 2.37396586479, 0.517644591976, -0.218383873097, 1.36512083985, -0.149858817182}, {2.45772674502, 0.62586294997, 0.0452530880392, -1.17538652629, 0.27547438634, -0.592234162187}, {0.0240588522302, 2.62369485262, 0.100936094225, -0.338009204746, -0.0255255291114, -1.9193601972}, {0.726189685589, 0.62586294997, 0.0229103724326, -0.816510531344, 0.321672735552, 2.72558092535}, {0.0955364916137, 1.87450788913, -0.0811395889959, -1.41463718959, 0.149964434302, 2.06201790784}, {0.821199524212, -0.872510977019, -0.0109804159683, -1.89313851619, 0.178419935912, 1.17726721783}, {1.88587652264, 1.6247789013, 0.594185570837, -0.696885199695, 1.15557931831, -1.2557971797}, {-0.287236014889, -1.37196895268, -0.0926446010917, -1.17538652629, -0.144683755839, 1.39845489034}, {-0.522298016061, -1.37196895268, -0.00754361340645, 1.09749477505, -0.328342326418, -0.371046489685}, {-0.695083471306, 0.875591937801, 0.0233079162071, -1.29501185794, 0.0497311295857, 2.06201790784}, {-0.533928864717, -1.37196895268, 0.238349517673, -1.29501185794, 0.0315406800348, 2.50439325285}, {-0.720870564593, 0.875591937801, 0.0163781086838, -1.41463718959, -0.243369525117, -0.149858817182}, {-0.760634322631, 0.376133962138, 0.0213211799202, -0.0987585414474, -0.199535191986, 2.50439325285}, {-0.622715391984, 0.376133962138, 0.0876330588243, -1.29501185794, -0.223619147318, 0.956079545331}, {-0.687047032426, -0.872510977019, -0.0689856071859, 0.977869443398, -0.198712479469, 0.956079545331}, {-0.791160175987, 0.376133962138, -0.174058636941, 1.09749477505, -0.169567087746, -0.81342183469}, {-0.653949372298, 1.12532092563, -0.145238610879, -1.53426252124, -0.259241772795, 0.956079545331}, {0.553632903775, -0.622781989188, -0.153642183863, -1.29501185794, -0.234130057545, 0.734891872828}, {0.139141705056, 0.126404974307, -0.0813952429952, -2.13238917949, -0.250772942067, -0.149858817182}, {0.633027257129, -0.872510977019, -0.219614283443, -0.457634536396, -0.201349902467, -1.2557971797}, {0.961709380481, -1.37196895268, -0.0446523256362, -1.05576119464, 0.117188204939, 0.956079545331}, {1.43226410213, -0.622781989188, 0.166740327273, 1.33674543835, 0.11340782423, -2.36173554221}, {1.33831890174, -1.37196895268, -0.00614900327347, -0.218383873097, -0.0585995164683, -1.03460950719}, {0.860708867572, -1.12223996485, 0.00544036370442, 2.29374809154, 0.194934180662, 2.28320558035}, {0.895046618045, -0.622781989188, 0.0435736457197, -2.25201451114, 0.000178630099459, -0.149858817182}, {0.786907149013, -1.37196895268, -0.0978203652847, 0.858244111749, -0.174482438809, -0.592234162187}, {-0.141355959784, -0.622781989188, -0.239858476071, 0.140492121852, -0.240805652898, -1.03460950719}, {0.101790045897, -0.622781989188, -0.203854820466, -1.29501185794, -0.15899572026, -2.36173554221}, {0.319007347355, -1.12223996485, -0.208548500287, -1.89313851619, -0.13844550098, -0.149858817182}, {0.376754580013, -1.12223996485, -0.1984035341, 0.0208667902021, -0.24695242526, 0.956079545331}, {0.0597304285334, -0.872510977019, -0.335667718377, -0.218383873097, -0.247086513739, 0.0713288553204}, {1.27233462265, -1.12223996485, -0.30313278327, -1.17538652629, -0.330097099414, -1.2557971797}, {0.105122255342, -1.12223996485, -0.196671581581, -0.0987585414474, -0.155502410003, -1.6981725247}, {2.13548057249, -1.37196895268, -0.176834326586, 0.977869443398, -0.238439740326, 0.956079545331}, {-0.415248515388, -0.373053001356, -0.338971727913, 0.379742785151, -0.382430142894, -0.371046489685}, {-0.158607303668, -0.622781989188, -0.320828070673, -0.218383873097, -0.370076786423, 0.734891872828}, {0.410032268035, 2.12423687696, -0.0292941024306, -0.577259868045, 0.0400258216174, 2.28320558035}, {1.37452592245, -0.872510977019, -0.13726134078, -0.577259868045, -0.0124278461339, 1.39845489034}, {1.37452592245, -0.872510977019, -0.13726134078, -0.577259868045, -0.0124278461339, 1.39845489034}, {3.04083121803, 0.376133962138, 0.155566700044, 0.260117453501, 0.440313587791, 0.734891872828}, {2.45772674502, 0.62586294997, 0.0452530880392, -1.17538652629, 0.27547438634, -0.592234162187}, {4.78901204009, -0.622781989188, 0.1324846298, 1.33674543835, 0.459682828156, 0.0713288553204}, {4.06561622494, -1.37196895268, 0.0285335560203, 1.6956214333, -0.0539233113148, 1.61964256284}, {3.00540199912, 0.376133962138, 0.0232179785702, 1.09749477505, 0.0792824344919, 0.513704200326}, {1.8105523006, -0.872510977019, -0.114068932497, 0.61899344845, -0.22252994233, 0.956079545331}, {1.27301888747, 1.37504991346, -0.125179118296, -1.53426252124, 0.345602350812, -0.81342183469}, {1.23767530061, -0.872510977019, -0.205482756711, 0.61899344845, -0.226957176549, 0.0713288553204}, {0.892096858345, -1.37196895268, -0.164031111482, 0.260117453501, -0.0623210697258, -0.371046489685}, {-0.157130888102, -1.37196895268, -0.473173694521, -1.29501185794, -0.584722122542, -0.149858817182}, {-0.779825524453, -1.37196895268, -0.475165518903, -0.816510531344, -0.61786230215, 0.292516527823}, {-0.624275683648, -0.622781989188, -0.473714306012, -1.29501185794, -0.623864084421, 0.734891872828}, {-0.688044115523, -0.872510977019, -0.454199243143, -0.457634536396, -0.613219704433, 0.734891872828}, {-0.773854564655, -1.37196895268, -0.465752062863, -0.218383873097, -0.579374467556, -0.149858817182}, {-0.864234343683, -0.622781989188, -0.476093188164, -0.696885199695, -0.591827515826, -0.81342183469}, {-0.852641938846, -0.622781989188, -0.494676935426, -1.05576119464, -0.588020076997, -1.4769848522}, {-0.899487315929, -1.12223996485, -0.489565531387, -0.457634536396, -0.588874663215, -0.371046489685}, {-0.845460007537, -0.123324013525, -0.488563086314, 0.858244111749, -0.571228443601, -0.149858817182}, {-0.858837243937, -0.872510977019, -0.480131014662, -1.17538652629, -0.559230470639, 0.956079545331}, {-0.824070520212, -0.872510977019, -0.476201619381, -0.0987585414474, -0.584765443645, -0.371046489685}, {-0.971266136811, -1.37196895268, -0.491823018565, -1.53426252124, -0.592962301639, -0.149858817182}, {-0.938904323864, -1.37196895268, -0.489773027158, -0.936135862994, -0.586254841755, -0.81342183469}, {-0.956269445137, -0.622781989188, -0.496994688003, -1.17538652629, -0.585902588321, -0.81342183469}, {-0.903304246495, -0.622781989188, -0.490574468893, -0.577259868045, -0.574326568319, 0.0713288553204}, {-0.903373061842, -0.622781989188, -0.489602832427, -0.457634536396, -0.582372434608, 1.17726721783}, {4.69138827931, -0.123324013525, 0.299826228196, 0.260117453501, 1.21432870152, -0.149858817182}, {3.47520772535, 0.376133962138, 0.51618761719, -0.218383873097, 1.18596535196, -0.371046489685}, {4.04052189985, -0.123324013525, 0.43654553015, -0.577259868045, 0.836268297592, 1.84083023534}, {2.13432019425, 0.875591937801, 0.317421926642, 0.858244111749, 0.65657188247, 0.734891872828}, {1.01101160738, -1.37196895268, -0.0817381120187, 0.0208667902021, -0.13182005044, -0.592234162187}, {2.30138812331, -0.872510977019, -0.132182501943, 0.379742785151, -0.130092714282, -0.81342183469}, {1.9194566764, 0.376133962138, 0.0304196130687, 2.29374809154, 0.319629584988, -0.149858817182}, {0.200159744687, 2.62369485262, -0.0364259129279, -1.29501185794, 0.373360326178, 0.956079545331}, {1.38509692121, -0.123324013525, 0.0620761773242, -2.01276384784, -0.193606987741, -0.371046489685}, {-0.715537888637, 0.376133962138, -0.265243568986, -0.696885199695, -0.0244390356272, -1.03460950719}, {-0.734391620722, 2.62369485262, -0.243951481733, 0.0208667902021, -0.0501872885145, -1.4769848522}, {1.4776038789, 1.37504991346, 0.420498941715, 1.45637077, 1.05819358591, -1.9193601972}, {-0.257367304505, 0.875591937801, -0.227390820927, 1.81524676494, -0.277125850529, 1.61964256284}, {-0.276073056924, 0.62586294997, -0.298958692748, 0.61899344845, -0.468138153342, 0.734891872828}, {-0.449629524516, -0.872510977019, -0.29027480103, 1.09749477505, -0.449880341731, 1.61964256284}, {-0.555194394626, -0.373053001356, -0.382853556455, 1.09749477505, -0.448786602839, -0.592234162187}, {-0.160457155071, -0.123324013525, -0.271364016979, 1.2171201067, -0.287497904666, -0.149858817182}, {-0.255327114143, -0.373053001356, -0.287282950315, 0.977869443398, -0.268281430492, 0.734891872828}, {-0.149244153229, -0.622781989188, -0.249557138485, 0.260117453501, -0.29921059491, 1.39845489034}, {-0.185709188671, -0.373053001356, -0.304118911185, 0.4993681168, -0.415608203293, 2.28320558035}, {0.0998549020767, -0.373053001356, -0.301673337327, -0.218383873097, -0.431754846921, 0.0713288553204}, {-0.159781550445, 0.376133962138, -0.234939310352, 1.33674543835, -0.302408268198, -1.2557971797}, {0.690615800071, 0.126404974307, -0.313028007893, 1.2171201067, -0.414143687176, -1.6981725247}, {0.200027156751, -0.373053001356, -0.252325525817, 0.977869443398, -0.456313412025, 0.956079545331}, {0.06470231013, 0.126404974307, -0.359493604706, 1.33674543835, -0.42101501355, 1.17726721783}, {0.243101161437, -0.123324013525, -0.384920267683, 0.0208667902021, -0.445667236779, 0.734891872828}, {-0.213750702378, 0.62586294997, -0.304042523001, 0.260117453501, -0.402364717234, -0.149858817182}, {0.0116197753668, -0.373053001356, -0.256409900923, 1.09749477505, -0.341991420031, -0.81342183469}, {0.0545225511394, -0.373053001356, -0.253140687227, 2.17412275989, -0.348218395669, -0.81342183469}, {-0.081611694209, 0.376133962138, -0.401064537611, 0.61899344845, -0.419129781168, 0.956079545331}, {0.00022548583785, 1.12532092563, -0.377744154487, 1.81524676494, -0.438321992131, 1.17726721783}, {-0.145252543508, 0.62586294997, -0.322118443005, 0.379742785151, -0.425873702066, -0.592234162187}, {0.383669946375, 0.875591937801, -0.361462956334, 0.140492121852, -0.472610041622, -1.9193601972}, {0.166231046576, 0.126404974307, -0.402375116691, 0.858244111749, -0.379557877322, 0.513704200326}, {0.115221980726, 1.12532092563, -0.252736122873, -1.77351318454, -0.334848140592, -0.592234162187}, {0.509814982338, 0.376133962138, -0.340904264708, -0.577259868045, -0.367607937475, -2.14054786971}, {0.793058711628, -0.373053001356, -0.29646487528, 0.379742785151, -0.456390462855, -0.371046489685}, {0.695070190492, 0.875591937801, -0.324817355028, -1.05576119464, -0.372907806206, 1.39845489034}, {0.810251280073, 0.875591937801, -0.35178034273, -1.77351318454, -0.398711891727, 0.513704200326}, {1.63690538578, 0.875591937801, -0.281943461721, 0.4993681168, -0.344502108231, -1.6981725247}, {0.723416580372, -0.373053001356, -0.236226644945, -0.696885199695, -0.349630767305, -1.4769848522}, {0.330982856152, 1.6247789013, -0.213854410593, 0.140492121852, -0.347373321344, 2.28320558035}, {-0.12342420924, 0.376133962138, -0.20844810539, -0.577259868045, -0.390593852062, 0.513704200326}, {-0.413079132522, 0.62586294997, -0.283128983059, -0.0987585414474, -0.290894155695, -0.592234162187}, {0.0868790290886, 0.376133962138, -0.327999838583, -0.0987585414474, -0.383282294667, 0.956079545331}, {0.178834416684, -0.373053001356, -0.254004738944, 1.2171201067, -0.432385881329, 0.734891872828}, {-0.41565390032, 0.62586294997, -0.336070486652, 0.61899344845, -0.41308172952, 0.734891872828}, {-0.293995815737, -0.872510977019, -0.27432011658, -0.0987585414474, -0.421813831921, -0.592234162187}, {-0.151655203789, 0.126404974307, -0.321065533204, 0.0208667902021, -0.456099042825, -1.2557971797}, {-0.147894742959, 0.62586294997, -0.351762658825, 0.61899344845, -0.458531490921, -1.2557971797}, {-0.231663389809, 0.875591937801, -0.362233279599, 0.977869443398, -0.423466988493, -1.03460950719}, {0.229638879182, -0.123324013525, -0.335575881847, 0.977869443398, -0.444240122643, -0.149858817182}, {-0.385225294413, 0.875591937801, -0.326571597206, 0.977869443398, -0.367274422978, -1.6981725247}, {-0.520196422379, 2.12423687696, -0.302918824159, -0.338009204746, -0.391230661236, -0.81342183469}, {-0.424237803385, 2.12423687696, -0.299149893737, 0.738618780099, -0.369285641851, -0.149858817182}, {-0.537424720004, 2.12423687696, -0.314995932311, 0.977869443398, -0.318174115758, 0.956079545331}, {-0.352426636404, 1.37504991346, -0.332551875744, 1.45637077, -0.323236585691, 0.292516527823}, {-0.256483417296, 0.126404974307, -0.302531504504, 1.45637077, -0.341794286729, 0.513704200326}, {-0.264340383474, 0.62586294997, -0.267943246722, 0.0208667902021, -0.371184323855, -0.81342183469}, {-0.350027061977, 1.12532092563, -0.297638118551, 1.09749477505, -0.395999525271, -0.371046489685}, {-0.444794046423, 2.62369485262, -0.35645558778, 0.738618780099, -0.351977105251, -0.371046489685}, {0.0138140704233, -0.373053001356, -0.352560381979, -0.696885199695, -0.366355734165, -0.592234162187}, {-0.21163694399, 0.126404974307, -0.32804764677, 0.738618780099, -0.336135985202, 1.39845489034}, {-0.179569297608, 0.126404974307, -0.334876653737, 0.977869443398, -0.30383643473, 2.72558092535}, {0.0366121318294, -0.622781989188, -0.353046687163, 0.379742785151, -0.36007787212, 0.513704200326}, {0.316711345305, -0.373053001356, -0.357551255174, 0.0208667902021, -0.411056650037, -0.81342183469}, {-0.0122192404181, -0.622781989188, -0.390715775011, 0.0208667902021, -0.39846277847, -2.14054786971}, {-0.0156049633181, 0.875591937801, -0.370231029478, 1.33674543835, -0.48399524945, 0.513704200326}, {-0.352420125065, -0.373053001356, -0.372207073054, -0.0987585414474, -0.475049040039, 0.956079545331}, {-0.236252389161, -0.123324013525, -0.380087690302, 0.0208667902021, -0.489331020253, -0.149858817182}, {-0.206658166651, -0.123324013525, -0.395274242872, -0.577259868045, -0.491023558814, 0.734891872828}, {-0.368568331052, 0.126404974307, -0.39211439619, 0.738618780099, -0.487601662597, -0.371046489685}, {-0.0818505825424, -0.622781989188, -0.31623845804, 0.977869443398, -0.40326603032, -0.371046489685}, {0.133391735346, -0.622781989188, -0.333848945092, 0.977869443398, -0.34866184047, -1.6981725247}, {-0.294529869926, 0.875591937801, -0.303006843429, 1.45637077, -0.363459164778, 0.0713288553204}, {-0.230995437182, 0.126404974307, -0.299762465577, 0.858244111749, -0.446135789802, 0.292516527823}, {0.271572707008, -0.373053001356, -0.332808432742, 0.858244111749, -0.339062784985, 2.28320558035}, {0.20213592896, -0.373053001356, -0.331086900863, 0.260117453501, -0.366160601878, 1.39845489034}, {-0.283895322966, -0.872510977019, -0.314258123702, -0.0987585414474, -0.388350549909, -1.03460950719}, {-0.493446210262, 0.62586294997, -0.211636808872, 0.140492121852, -0.240883020945, -1.6981725247}, {1.37097756738, 0.126404974307, 1.55379921685, -0.577259868045, 1.23773507633, -0.149858817182}, {1.06626324264, 1.87450788913, 0.437541918682, -0.696885199695, 0.748252183274, -0.149858817182}};
            double[][] coefficients = {{-1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -0.244665850888, -1.0, -0.284930251747, -0.591726201374, -0.342775330132, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -0.0112386053748, -1.0, -1.0, -1.0, -0.635129767788, -1.0, -1.0, -1.0, -0.674092363125, -0.764045249251, -1.0, -1.0, -1.0, -1.0, -1.0, -0.82206552493, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -0.0745830102177, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -0.695335106407, -1.0, -1.0, -1.0, -1.0, -0.73264742346, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -0.123111831404, -0.153553718004, -0.204787923863, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -0.221942532557, -1.0, -1.0, -1.0, -0.861571297009, -1.0, -0.27014814512, -1.0, -1.0, -1.0, -1.0, -0.607854566983, -0.110991019369, -1.0, -1.0, -0.00276693329496, -0.0601574091114, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -0.0288487008213, -1.0, -1.0, -0.417952841133, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -0.726983069271, -1.0, -1.0, -0.533139239267, -1.0, -0.694266872933, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -0.860956540784, -1.0, -1.0, -1.0, -0.0411951012993, -0.0387742917597, -0.0587422193235, -0.0176185917597, -0.0593637340895, -1.0, -0.141271167392, -1.0, -0.33914147051, -0.278444559391, -0.0170220066521, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -0.401571498483, -1.0, -1.0, -1.0, -0.118064091982, 0.133585992969, 1.0, 0.874231303657, 1.0, 1.0, 1.0, 0.258504167984, 0.355686993616, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.806428464189, 0.352387744688, 0.875551361867, 1.0, 0.851581887391, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.869409529706, 0.313368765271, 1.0, 0.710326429782, 1.0, 0.529298168347, 0.741418650798, 1.0, 0.546410561651, 0.370338351945, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.618740102519, 1.0, 1.0, 1.0, 1.0, 1.0, 0.361797190408, 1.0, 0.0612804998021, 0.322490658438, 1.0, 1.0, 0.701858445817, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.668615865243, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.905835226388, 0.612863839499, 1.0, 1.0, 0.904409228996, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.267560399085, 1.0, 0.249496228208, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0}};
            double[] intercepts = {0.843111224311};
            int[] weights = {164, 150};

            // Prediction:
            SVC clf = new SVC(2, 2, vectors, coefficients, intercepts, weights, "rbf", 0.166666666667, 0.0, 3);
            int estimation = clf.predict(features);
            System.out.println(estimation);

        }
    }
}