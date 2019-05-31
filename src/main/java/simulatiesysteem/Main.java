/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package simulatiesysteem;

import com.google.gson.Gson;
import com.rabbitmq.client.AMQP;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import simulatiesysteem.jms.gateway.MessageSenderGateway;
import simulatiesysteem.json.RootObject;

/**
 *
 * @author Ken
 */
public class Main {

    private static final String BASE_URL = "http://192.168.24.14:5000/route/v1/driving/%7.6f,%7.6f;%7.6f,%7.6f?overview=full&geometries=geojson";
    private static final double START_LAT = -0.5;
    private static final double START_LONG = 44;
    private static final double END_LAT = 3;
    private static final double END_LONG = 48;
    private static final int STEP_TIME = 10;
    
    private final String[] trackers;
    private final Gson gson;
    private final Random random;
    private final MessageSenderGateway sender;
    private final AMQP.BasicProperties props;
    
    private Set<Simulation> simulations;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        if (args.length <= 1) {
            System.out.println("Invalid arguments specified.");
            return;
        }

        String argument = args[0];
        if (argument.equals("-v") || argument.equals("--vehicles")) {
            argument = args[1];
            int amount = Integer.parseInt(argument);
            if (amount <= 0) {
                System.out.println("An invalid amount of vehicles was specified.");
            } else {
                new Main().run(amount);
            }
        } else {
            System.out.println("No vehicle amount specified.");
        }
    }

    private Main() {
        this.gson = new Gson();
        this.trackers = new String[]{
            "FR_6bf0df3b-aa04-4340-803a-acd92c0c6ef5", "FR_8edf8a71-bce5-4a85-91d2-c321ee423c0c", "FR_f49aef3b-f27a-434b-89cb-9f7c4a3c88aa", "FR_1b25b785-53ff-4abd-8ac1-cbc3dc1047cd", "FR_bafb5bd5-e3e2-406b-823b-63eba03b303c",
            "FR_c5c1ae90-f94a-4f5c-9da3-c7aadc5c1c54", "FR_4ec7743f-7b4a-4078-9b33-3ec56646d8b1", "FR_8641e6ff-67d7-401c-aace-6542dc8b5172",
            "FR_86aaa978-9c23-430d-ab7f-509722014b3c", "FR_cf70622c-3ae9-4a1a-99bd-efd4d68b17ab", "FR_b779796d-2932-414c-a071-4af7e2a0f857",
            "FR_4cd278f3-55a7-4f19-9c17-dce2beb859d5", "FR_126f35c5-00b7-4168-be47-f5586a83223e", "FR_66dfefae-7d63-4303-8f77-cd15ff584892",
            "FR_6af354f2-bd64-463f-9e7b-c0a680794618", "FR_c6293fcc-7105-42b7-991d-27678c93b1fe", "FR_48b1032c-a467-4727-8836-9f004c5f5be2",
            "FR_a04daae6-5e87-4f3c-9783-bc977164a8eb", "FR_1fb56ed6-9a26-4121-92c6-341348821074", "FR_a2e761bd-5c91-4492-9196-23b900bf18d4",
            "FR_9c675c8c-340f-4b84-a0e8-1abae691a467", "FR_eca2d2de-e1b0-4fd9-b7bb-4ad60901795a", "FR_cc2bc21b-0771-4666-9d66-e562505701b3",
            "FR_931e3712-177b-4b14-8c13-33a0f4056b48", "FR_9afd2c82-7209-4d13-949f-e9d541b99148", "FR_f89c5c54-21f1-498b-8bfb-2e80129f76a0",
            "FR_ee57d727-00d5-44ad-8767-76c0aef37a0a", "FR_851f89ed-1816-4edd-8c9f-e8c03ac57f1a", "FR_31d9fefd-d83a-4c1c-8415-21353e9cf277",
            "FR_804b87c1-f1d1-4816-9fcb-43440097bd45", "FR_92df9312-7e4d-474c-8bd9-4ebc5625ec9b", "FR_0d3497d3-b075-49ab-b082-936d2cff0d2b",
            "FR_ba3397ab-6a45-4570-b88d-79c7f3a441fc", "FR_a67bed26-a98e-4733-a2bf-3165fdd9f7a8", "FR_cd072343-eaf3-42d4-aae3-e3cf46227976",
            "FR_be672daf-2b94-40c5-be26-3688fead034f", "FR_a1775036-ae5f-4c69-a04d-271aa910b1d1", "FR_0bbaf785-89e5-4ee6-a4cb-80a0000b8aa3",
            "FR_d0ff5e94-ccd9-4a02-8d90-858633f8c6e6", "FR_96f6c5f5-9ab7-4c7c-bc7e-8a47fdb62bab", "FR_182a4e59-19e0-4209-a99a-6e7351db09b2",
            "FR_fb5d75f7-35db-4d70-b93f-db03fb168963", "FR_c3651835-8092-4090-930c-50e90f830408", "FR_8ef2da5e-ed67-4f03-9094-28cd7fee9087",
            "FR_32d873fa-0b66-415a-b34b-222a465afe09", "FR_c31144ad-b1f2-42b9-abb6-7f58647ea2eb", "FR_c02f4d0a-0e5a-404b-945c-2fae2da2ff8b",
            "FR_f4a7c2bd-ed2a-468e-a22b-8db657f92d88", "FR_1cd4d83c-3bf2-4532-8484-551aa93b4e40", "FR_158ef5a6-f192-4bde-b3f4-09ef3f836591",
            "FR_a8cfff53-ebcf-47d5-b348-8a97e546ce0f", "FR_9c7a453c-05a4-48ae-9641-5a324a298a1f", "FR_9896c713-1252-4aa3-9e1c-86c7c2ed47b5",
            "FR_24197b94-3a1d-4b81-9638-d5bf1a3d4734", "FR_d936a43d-3b2d-4b40-9a69-d9b8b8e6ffb5", "FR_8389e133-8bc8-46e6-9555-54344413a201",
            "FR_8e01a9c0-20fc-4a49-ab17-7538a4eee85c", "FR_acafb3d1-b489-47db-84ce-27d089edbebe", "FR_acf820d1-6b68-4a4e-9105-0d868748f2ca",
            "FR_830d79aa-87d6-4971-9124-a7c44ecef26e", "FR_62cc2927-e037-4182-80e8-14a6666f867b", "FR_71824ec1-4e3b-4b11-81fa-44e11f35db50",
            "FR_78cc4dc0-a1bf-473e-8182-f297bd556cf6", "FR_bec65e08-212f-4f8a-8b09-3a489e4b3979", "FR_bef2ca30-e021-4a89-9291-84014b535c1d",
            "FR_a1a394e6-0fa9-4af9-9c3f-5fed586fb634", "FR_17d1ad1e-b272-4b91-867c-0f7c373f6658", "FR_e8d7a76f-e766-45fe-a17b-c8d0dd9b45a4",
            "FR_da2b85c0-e48e-4e90-b007-10a180e8f08d", "FR_0295fd7f-e200-4a7b-84ba-2eadec04fdc5", "FR_469e8616-4b4e-4d2b-b7c9-d486127bfa4f",
            "FR_be1153c8-50e1-477b-827c-3248ad85ae8c", "FR_d16037f6-b302-472a-aac0-fde0e95199e9", "FR_bdbfacb5-5413-429f-a5f7-7ceddf89e967",
            "FR_6767d740-3332-4dac-bfce-7721b6c646f8", "FR_f5614ba6-384d-4f58-8777-f7f08b34cf55", "FR_c6a3fe2e-e075-444b-a7f7-1c9829890be2",
            "FR_a9164039-a06c-461f-a0a7-49b702101aab", "FR_c0a2e362-ff0f-4e31-ac7a-21ff6e872c03", "FR_7c44f0ad-6e1e-4f2a-9228-ea5a716cf4d0",
            "FR_218ce4a0-1c5a-48dd-98da-b50b0046c448", "FR_a0a61e58-b69e-40d4-ad69-18f9eb22980e", "FR_30c9ce05-aea7-4bc6-928c-25887f38a37f",
            "FR_03422c57-d536-4c25-8a81-2f23df6b3374", "FR_64d32df8-b5b9-4d2f-b1a3-d296b0e085c0", "FR_fa225d37-8a47-41b6-8536-0c84eb5684ec",
            "FR_48e2cf30-d4e0-448a-beb8-395bd305745b", "FR_17cbcba4-2795-4509-acf4-820c6f1d99f5", "FR_19292469-645d-4f3a-ba94-fe2982de0bd5",
            "FR_3fd4a315-1001-4931-a3fb-e170903b082a", "FR_c4cc98ea-12bc-4ccd-9da3-3e4d288fa5bf", "FR_db33190f-1af7-45dc-97a0-27b68e070410",
            "FR_50d978e3-92e3-4e2d-8c1c-467b12062443", "FR_7c444da8-9aba-4f52-b292-c83823c7ccae", "FR_9c5377cf-36a9-466b-9068-72c189ba3a4c",
            "FR_fc3cc415-394a-4388-a0e4-7326dc3f7fe3", "FR_48afae07-5d3d-4bac-b22f-2a06cd02fafc", "FR_8437776c-f3a3-4b03-8120-b1b720c54d65",
            "FR_8d06aae1-fbaf-42c7-adaf-4919eb8496e0", "FR_d2873b77-4380-405b-ac6c-e48c1d994193", "FR_cb8ba20b-ab16-4991-9d62-da6e89f460bc",
            "FR_d01c7c9e-12ae-4bf5-8750-8c76a29f0f18", "FR_3f7951da-697f-4d08-bad3-199daeaa9c3b", "FR_bfa1ceb4-e744-435e-ae08-ab0c29424354",
            "FR_d053d17d-5c2f-4a9f-8eec-9b615e944057", "FR_1eb275f2-8b8f-4f92-9bec-d663c360f953", "FR_dd40b5c6-200e-4a88-8d96-2813f2f178ab",
            "FR_ea937972-82db-4779-89e2-4ae2a4d1ca83", "FR_68dc4b37-bd09-4dea-a394-bab44fcee601", "FR_cf0a1407-8601-470b-b95a-829b97adc949",
            "FR_58c82cc1-6db9-4345-8c01-cded0ca45da3", "FR_b308e3e6-d5fe-4de2-881f-877dcfd30833", "FR_4e2a9c80-e82e-40d8-9249-bbd14be3109f",
            "FR_21d320bc-ada4-468a-b0ba-15eba53719d9", "FR_d0a847a1-d1a5-4ddf-965f-e69939f47b19", "FR_4f69c182-4e67-44a2-bf3e-ee6782980bc3",
            "FR_5af5ea40-affa-4119-9ef8-afbb8ed02791", "FR_6204faa1-076d-41f7-944f-b965f28dd912", "FR_ff73d6bc-ebd1-4b02-912b-71d76bce8fd6",
            "FR_d0900d1a-8935-4fbb-80b9-eb17ec524d60", "FR_1a313bdb-df23-48eb-8ef9-0205b7c6418f", "FR_f5b6d172-1a29-4e17-9198-98546cb9de55",
            "FR_ac43d9ed-825e-4d63-9cb3-a1d0d0422294", "FR_7724da92-af77-49ed-873a-01b5f4887360", "FR_5def7e67-93d2-4167-afdf-ddb9eb1e8599",
            "FR_98ff0247-ecb0-4ffe-a972-4511378c2317", "FR_ad52a331-a7f4-46e4-9951-24ca0c728005", "FR_68407b26-29f2-40ea-9875-a2b8aa30eea4",
            "FR_6e08fafb-a3c3-45d8-8b45-b3ac5b207b68", "FR_5ae87750-36df-4512-9650-17c2276098f8", "FR_c515edb5-6bb7-44be-8f29-4dbbbc8f29f7",
            "FR_8c08ee7d-c7eb-45b1-a421-9099c6519632", "FR_a12c748c-0ee4-4402-bbfd-45205ec25eed", "FR_7711d8cc-cb1d-4847-99b4-1ae13f3c1b98",
            "FR_3be433f7-5941-49f3-8c1a-706a6cbb0a80", "FR_eb67e58d-932a-43fc-a7ec-0b6c70b9898f", "FR_0f79c073-c670-4e31-8754-1bb2a172953f",
            "FR_3de5946d-b3b1-4197-b772-8947cd1562e5", "FR_19fd61b6-d18c-44e2-b6fa-9f74726fdb36", "FR_6dd003f7-78ce-413d-bc00-3fc49dd2b498",
            "FR_df096215-15c7-411b-b231-975cdb7e1d8a", "FR_8fdd7b92-23ac-4006-b1ab-09bb02684182", "FR_9db30a10-7b18-4867-9f57-b1943920a7d8",
            "FR_e55d3527-8a0c-4e4b-b9b0-6f1d6eeab885", "FR_d792bf6e-d20e-4ca5-a1de-7396e2605950", "FR_fcc5b37b-19da-448d-8bce-5d8713459c20",
            "FR_2605d92c-6c60-4499-ab63-d9050be5a6e4", "FR_0a6a1898-97ad-417c-a637-10a7c38f9910", "FR_5366d415-4f90-4216-8bc6-2e27eca56161",
            "FR_9a85d10a-1dc6-47fd-bdf5-d9ac70cec063", "FR_7d1f77cd-dda7-4e2f-895e-f132e81c4b8c", "FR_9c53c58e-e405-42e7-92ed-9bf9af0075f2",
            "FR_80ace095-8b75-4b35-899a-3a7772555462", "FR_aff2005f-f9c3-48ba-980a-583564c366f7", "FR_3d579482-1926-4b4b-87cc-18dacc7ea577",
            "FR_3197cf55-39ec-4046-8ffe-ee649738302f", "FR_21c8f04d-a426-40d5-89b8-04e038fd3981", "FR_408a2675-ddea-4496-a7c8-462a67720392",
            "FR_66500bcf-ac39-4aa6-a6e3-aa280dcca734", "FR_0a0dcb68-53c4-4e34-b189-fc8cb31c4ada", "FR_b6eb9fb9-b368-4cdb-86c0-e7cc79386759",
            "FR_1f96e729-4054-49ef-b2fc-92298176bbbb", "FR_bd93d56c-0c50-4bc3-bcec-e68731785985", "FR_4f016f8c-6ea0-472e-b3e8-9b2608693956",
            "FR_4f0078bd-633b-4585-8c9c-9747a0cba311", "FR_3977ad14-8cfb-449e-ab92-9515023b3a5b", "FR_366f9521-febe-4401-8795-37a3a2733cfc",
            "FR_12a74b2e-ba47-4d1e-844e-51eb8b0e4c19", "FR_42910899-367f-4107-9e65-20cbf15db8c1", "FR_0b4f21ee-c974-4c79-9a82-b6ec20f34bb5",
            "FR_026233e0-89dd-4066-a98d-80b08239ac00", "FR_896c8663-2e41-4ad9-8b76-f7da6188d6dd", "FR_91177f98-bf62-4adc-a6f0-3e10540222e7",
            "FR_d8d05803-2746-4f96-b953-d675bcaa95df", "FR_1b2f0e6e-43af-410f-bda2-a55971fe8834", "FR_b9d19f5e-6941-49c8-abeb-6575128f4628",
            "FR_da1fb27e-9811-4e28-a4ce-7f89f9f405d9", "FR_b0d90e30-353d-498c-bdbc-8711ab8d0d82", "FR_990ed23b-140b-425a-8e55-b0ff14d1334c",
            "FR_8b4c10bf-917a-420d-9db2-eafe0c0351c6", "FR_e7cb2417-8582-493e-b57c-d350934f1179", "FR_7e570abf-8614-4880-a60f-cdd12ab5da7a",
            "FR_70dfd745-4cc8-479a-bfb3-06236faa0cd4", "FR_4d827a76-2545-4654-8fe7-d09057867bed", "FR_dfbd51bb-d8e8-4e5b-9e0f-31c801655320",
            "FR_a4becbde-bf8a-4b69-9076-42560fd81936", "FR_057e0b33-b9dd-4648-8406-f0cb2e3ec01a", "FR_cd846f83-9d86-4f3b-9a7d-6ec5ebb53975",
            "FR_a9305955-fcbd-4ff2-affe-52cb0c44269f", "FR_b4d571d3-2a50-4c4e-8cb3-58b278efcdec", "FR_28df8a2b-f3f8-4862-be02-98aa51da3067",
            "FR_81fc0396-693d-46b3-9c3b-dec1208b71f4", "FR_46d5f5fa-7d56-45c5-8744-32c69d6ad038", "FR_6c9e20c4-aef3-4099-a3a3-550b192c2264",
            "FR_6699fd26-580a-4505-bba3-b7054ddcad7a", "FR_aee28264-99cf-4537-a0bd-34d8cc0baca9", "FR_e5ff93f3-82a3-447f-b1c6-d0eada3a5f98",
            "FR_5872c394-4715-4650-a9e6-ab4112ecae88", "FR_308ab11d-4052-422b-9607-dcf88f0d5cf0", "FR_ed785b13-6bb4-4bcd-93a2-543e9fe54f21",
            "FR_41d5fc40-29f6-4249-bece-a3fc0641f87c", "FR_4d51496d-760d-4079-91d9-6178c7702ffc", "FR_71092fb4-aed6-4385-91c5-a7f410dc73a0",
            "FR_29e1b478-590e-421a-99a0-8c98ebe9d7d9", "FR_0c82beed-6a11-4138-a6c7-0b26e6fe9626", "FR_88989b2e-69c4-4f05-bf8f-86b286f951c1",
            "FR_6c8eaf20-5c77-4333-bfce-e7b19abdff28", "FR_77283272-d676-4af1-b378-cbb5de70bc71", "FR_a425d89e-e718-4b25-b858-b5ca711d225c",
            "FR_609fb16c-f6c2-4d8f-a04e-51298ddf8651", "FR_10f0be6f-ae8f-4ef4-88f1-34b4ce059d7f", "FR_25dd9d55-6e94-4375-ac11-69207c35eeaa",
            "FR_1a4781db-b398-4987-9a9f-c974bfffbf4e", "FR_744b8c26-2be4-4e24-862e-63c3b8ff6691", "FR_e49c04e5-7172-4896-b8a1-84dbfa6450c1",
            "FR_480c89d6-47ec-4fd7-b77e-1e84f5953806", "FR_93201b12-d9f6-4d52-b653-74baa1885df6", "FR_36a70745-2d54-4283-884d-ef4f5e840bc8",
            "FR_d40cad6e-47a3-40a0-9497-876cca75e689", "FR_258764f6-c95c-4f12-be89-7f0a6f078b34", "FR_ad8add3a-1711-4284-a14e-d1921bbc5939",
            "FR_7dcb6553-003e-4235-911b-21bcc4f7b5c0", "FR_0f70c103-bf82-461f-b2a7-23fdf25eea1c", "FR_a9bd15af-f620-4c31-a0cb-0a578e9ec6e7",
            "FR_90f23f9c-0e2e-4cd4-accc-ab75b06c417c", "FR_eb665229-23a6-4871-a2c0-8f1728ba2867", "FR_2e564729-98a9-4e15-b6b7-9a04bfe77544",
            "FR_c61eacd9-fbf7-4aef-b5ee-b90ee2e6a0ef", "FR_73fdab06-9dca-4d11-ab9b-9d24cea9dd9b", "FR_7ee754d5-0ef6-456e-99b7-826b03599428",
            "FR_e4740ec0-9998-48d4-a3ba-d59b6a419d8a", "FR_0e107bac-2c75-4c07-b083-7da49874451a", "FR_dfaccc40-f7ff-4a92-944e-8b33f3027563",
            "FR_54afe3cd-23da-48b7-a95b-660922fbafc6", "FR_967bc058-f21e-44e0-bf94-ec914691a1a5", "FR_2b459a2d-203f-4bca-8d10-251cfed75cb3",
            "FR_9f799827-92dd-4dce-ba81-a4558afe8dce", "FR_d04f22c3-8214-43ae-a3d2-a5f46f8cf4a2", "FR_fd269f87-f3fe-4c04-970e-1996393c7530",
            "FR_48a69e0a-bcaa-457a-937b-00b45039b515", "FR_e922bd30-3f6f-430e-a5b1-d905f26ba928", "FR_dcf7bef1-fb0e-4b7a-9252-c78fda4cee92",
            "FR_5df983dd-fa22-469f-80a7-e1c13768980a", "FR_7fd28112-5432-4cbb-9cde-504340a4db19", "FR_0a9b677c-431c-458b-8baf-d27c85c3c603",
            "FR_bb155d00-e944-41c1-baeb-1b5d111a4740", "FR_e8799b5a-9b8b-4ea0-9267-7b2b1631d716", "FR_7989b0ee-a38e-447d-ac28-ba7e20475945",
            "FR_de8324b7-6514-40d5-8b0b-785481c78d76", "FR_295c5732-49de-4afe-a8fb-42bd1ee4e1bf", "FR_d4dbbc5b-267d-4e07-8be4-2b8a6792dbb4",
            "FR_d4028aff-21c8-4990-a4b2-71d19a038612", "FR_05ffaf5a-23b0-40e6-9c5e-f27242e95c6a", "FR_1c8609ff-a943-4569-88ee-4f690af14991",
            "FR_0b0762b7-499b-472c-b0d3-9350d1cf4781", "FR_75c191aa-57b7-4846-9ad3-a449deb02dd6", "FR_b013a55a-f638-42fe-b0f1-2789c10b23ea",
            "FR_dbf84eae-80b8-45f4-af33-469f757705f9", "FR_f1d19b58-af29-40b7-80ca-e8ba79e07156", "FR_1c536b7b-617c-4af2-b809-1e14ff34fb18",
            "FR_7075f003-d80e-4202-ba10-f67f6892f28f", "FR_80ef3d8f-3296-4535-b8df-070b04b6beca", "FR_605f2c9b-1b13-474a-a651-0dab9db78bd8",
            "FR_ef1c4c28-af35-400b-afad-93ae607ffa6f", "FR_1793d792-478e-453c-862d-122d5ba7c2b3", "FR_d637f910-657f-43c5-ac39-40d1b8783e17",
            "FR_bee8fa69-82f6-4542-adf6-5994b004fc58", "FR_0a3b7f0e-4339-48d8-a7a3-59f4ed294f25", "FR_2efede4d-00ab-400e-84c2-302cee466261",
            "FR_5a8fabe1-77fd-416f-a97e-ff64fdb77ee1", "FR_de342f99-a83d-469f-bd0f-a9f78169b07e", "FR_61b50711-bd80-4202-af0a-27e873879e71",
            "FR_cd4af974-fbcb-4754-b7aa-b12fbd5fbd53", "FR_9cc680f8-2f8e-465d-b859-64fbe0f9c740", "FR_1bf9cd9f-1921-48dc-80ee-be7060926c00",
            "FR_2d570eb0-6ae1-4981-859b-34a949818cbb", "FR_f053be5d-4376-4fb8-a9e1-3a7ee188d06a", "FR_8573509c-f74b-4bb8-a4a8-c74281d8c963",
            "FR_3c7766ac-e60f-4d5d-abd9-872ad8617593", "FR_2e70f3a8-a4cd-4bfa-ae90-586a37911c6a", "FR_1a71c199-4885-42f2-816c-11ab6b603279",
            "FR_b104fb0c-f8a0-4f60-b7f5-c2c5c4120a15", "FR_aee9d2c2-3d45-4fbd-8073-cf318c5f21d9", "FR_a2b01a61-fafe-41da-a397-9f2cce15f7a6",
            "FR_93440610-03e7-433d-aae6-9499c365f3a3", "FR_ec3d5992-cf90-410e-ba62-4cb5b36e9c35", "FR_f84438f9-0bea-4a7e-a439-96e41bd4425d",
            "FR_08d5bd19-96be-4247-bcea-364d3edd59a1", "FR_a8214839-c676-4d04-9eee-ce00934e1d8e", "FR_fb6341ae-c1c6-479c-85d4-54bdf7a272b2",
            "FR_f9f5c7e8-7c56-4788-8a12-4cee16a58455", "FR_3784dfab-851b-4b0f-89bd-5099aec11db6", "FR_4acc4059-8746-4206-9592-607aaf7f2b64",
            "FR_eb4ae19a-336e-46a0-bfd1-1e0733ca3bad", "FR_b8cc3daf-e0e1-4d43-945d-7d28a8d3b72e", "FR_8ec0f06d-2995-4942-a3b2-f1935dcdf9a9",
            "FR_627c2cd0-9edb-45c9-83ec-8d0f82613a67", "FR_7a847e81-542b-4dae-9390-80da959341d6", "FR_01b94052-0c16-4654-a388-9ce7d59dd82e",
            "FR_a855c312-3eb0-40b5-86fb-a4c4d379af87", "FR_b2bbe620-b333-4948-9d09-ed2bf873abc0", "FR_833aa139-674a-489f-b9c5-b15fecd64423",
            "FR_b70372a3-9682-493e-b339-b3a4a7648bef", "FR_a7bbb01b-768a-472e-938a-4fac9777f9ca", "FR_b36bcaa3-0370-4ca1-ba23-f964c1fce59a",
            "FR_8b190931-1815-48b9-8046-219b07684b5c", "FR_d4c9764b-31ea-4659-a248-e6a286238b05", "FR_731c43ef-3106-4016-bb3e-9fa08173a3a6",
            "FR_c37c3750-c075-49be-b069-f39cfe97684d", "FR_8e1b3fb7-775d-4a3e-8ae2-53e43b657c17", "FR_652519f7-d8bc-4327-a81b-96a0771b6d3a",
            "FR_5537b606-21ff-461b-b9c3-22df37642311", "FR_fef585f0-1a9a-4c26-8964-6509db639fa9", "FR_c87bbfc1-3cc3-4ca0-ae9e-d9e833dc3296",
            "FR_d7d83deb-1067-464e-acee-8b89b5877948", "FR_a96d0cc0-1352-4d87-8074-4cab3a3ca690", "FR_9c2aaa3c-4516-458f-ad8b-a401b3333d16",
            "FR_8b1b4ceb-bc80-4a87-bc02-c99ff8ad101c", "FR_a214b025-4820-4692-9e22-0230dfb4c0c9", "FR_95522408-fca6-4d6a-b170-0e08e9d6224f",
            "FR_d7771517-35dd-43ea-8c06-18e7b702d178", "FR_506e04fc-9cfc-4abe-93b0-0e54ba5404de", "FR_57f4b280-bb4d-4843-92be-9b324bee65ed",
            "FR_7412d986-b467-4969-a9b9-d90724f0ac52", "FR_b0990207-dd5c-431c-82ed-ccabbf474610", "FR_95797a7b-f8a3-4f46-9a2a-1c955e53c88e",
            "FR_f3e67529-42cc-4103-8143-7b975f403651", "FR_aeec1ae2-d97d-4f01-8b15-544ea5453dae", "FR_56318ede-02bf-430b-b0eb-9926e382f437",
            "FR_98e0d93a-f1cf-48f4-bfb6-cba793fd634b", "FR_a3adc876-d76d-411e-bdcf-669556eaf967", "FR_19d724d9-2f8d-4277-a1fa-784be27f6504",
            "FR_e3ab458f-f546-48b9-9746-574c6a708e6c", "FR_37d97a20-e983-42f5-8c9d-63e2b592b37a", "FR_148ebbf9-45e4-4ad1-9efd-c269c6ee204d",
            "FR_631f39de-a123-44aa-9c2f-7abebbb0e23a", "FR_86288c6a-433a-470a-a7c3-f160873e5a4a", "FR_fdd950e1-6b10-4b96-ae41-562bd7a565e4",
            "FR_fe24de7f-9039-437f-8f07-16c2b353d682", "FR_d91f8ebc-62ab-447f-8bf9-7332c7f72408", "FR_83749fc4-e519-4116-a6f3-8c9fe2d93fd5",
            "FR_49502e3d-725f-4131-a5c0-ef1c2896c6fd", "FR_1af37a21-921c-4df0-8739-1757b467bf87", "FR_e0763d19-9b2a-4af2-a65a-f7f83d988e93",
            "FR_7185a6dc-1e1b-4e8a-8759-d52f6fb38451", "FR_8407165c-e143-4e5b-9e53-06ab0f4af2f3", "FR_ad0f1a3a-98fa-4131-8407-2f904f07fdb0",
            "FR_14b09985-40a9-40fc-be86-13dadfef49fd", "FR_f58670bf-d941-4f5a-8ce8-6f38e312761d", "FR_9552ce9a-b1d5-43c5-8df3-28f03a786b64",
            "FR_3090319e-09d0-4098-8a66-2b441f229fb7", "FR_7a8555ad-49a5-409b-a1e4-2a4517abc227", "FR_b9642a5e-4e45-4e71-bb8d-db04312957aa",
            "FR_990d08ac-a2bc-42e0-a0be-6b0b26ab0b47", "FR_7191d5ac-281b-4be6-81e5-f77d65d8bdfa", "FR_3b916a24-4543-4f59-9030-17aad4378fe3",
            "FR_73095d27-7145-45ac-8846-eae7b703b3d9", "FR_9bca37cb-a10a-4e36-b096-e1b8dc4ab9de", "FR_1c749b47-401a-4c3a-9e77-8e6052b3eb67",
            "FR_44a0ff43-4ca3-489d-92bb-f80cd6e5c0bc", "FR_d0079daa-4dd3-4858-a0f8-e20f3316a26d", "FR_1a147799-69cc-4f44-abea-11f8a084d073",
            "FR_27517a16-d148-434b-9085-e2aaee02edb4", "FR_75ed7950-e921-442e-8b8e-c0ced668e332", "FR_1daa14e2-787c-4a24-936c-b1faae3479c3",
            "FR_4cba5241-c054-4711-a073-c5b42ed3ef30", "FR_8ca4b101-6b7e-4bee-9110-554d8f3dd763", "FR_5a1c7a9d-ba3c-4dc1-bebe-db95345c7109",
            "FR_2a954405-6d43-4388-ac36-34d13c49dc5a", "FR_a71ca79c-dd0e-4f20-af9d-fc5b9a463618", "FR_5251593f-c95e-4041-b5ee-f0ed725b989d",
            "FR_2c675030-d8d6-49f4-a540-325d3558f9b9", "FR_c8dfaaa3-cce3-4d64-978e-f40e032666c3", "FR_96acd990-ac0e-42a1-a200-0ecf71ba6722",
            "FR_7ef803aa-de1c-4ba7-8b34-51abe60c11cc", "FR_33e0fc82-093d-4c5a-9866-9da8a8324171", "FR_06391111-4d82-4aae-95e3-f19a99976cde",
            "FR_729df3dd-df08-41ad-adfd-6ae048359974", "FR_366a36ab-501a-44ff-8bea-00709256f38e", "FR_9a4bdd1f-f13b-42b5-93ae-92a49d42f822",
            "FR_0d3bc603-c2e1-4428-a1dc-b8da89819d0e", "FR_a1ba2f9f-ea4d-408a-bcea-233f21a6646f", "FR_85705b9d-5531-41ba-a50b-cf8d3589f6a1",
            "FR_7de67556-ccaf-4651-a6fa-e8d5a017d718", "FR_6821b925-b96f-4c82-acab-744f6493139d", "FR_f31aa1ff-71eb-4d1c-a478-2285a7b1f079",
            "FR_cd6b7ff8-4935-4693-bfdb-52472546858d", "FR_5e86b347-9ceb-4c97-ae54-2203c54c98dd", "FR_d7fdade5-2c21-4e90-bb58-fcc5b3b0f742",
            "FR_a271ba9b-2e10-4da8-a2e4-1dcfc0079a78", "FR_bb14325d-77d5-4e84-97f4-bc2d9cc97db7", "FR_469be88a-c9a9-4d2f-acdf-509803439539",
            "FR_0e591a9f-3e7c-4fb2-bb85-0bc5dd5fdd09", "FR_5b44d91a-2ea7-4924-af6d-052317ff97b6", "FR_190bbb12-7900-4228-a3e7-841fc1ab50e9",
            "FR_0cb29844-aff1-46a3-a36d-b8f6bbc11bea", "FR_e493c2af-3a86-43c8-8228-cd8122acdfe5", "FR_5f0cedb1-7cc5-432e-98eb-97cbe31cac6a",
            "FR_f0d24129-8fd9-4bc5-b3d4-ae76e1c51d6c", "FR_8a065f10-d5af-4a82-aba4-a94c85f1aca5", "FR_72cbb2eb-0843-4b2c-9cc2-1b0f3c362b8a",
            "FR_60cfa5fc-cae5-4c8b-b925-244e27c6fc79", "FR_5bdc39aa-5dcb-4df1-8d90-4dca433d1c3b", "FR_6eaef3f3-275d-43fc-b6a4-db04d1ea265b",
            "FR_3905c942-71b8-4f2e-aee7-12f1f02bf0ae", "FR_925dcd9f-c232-4d29-980d-9c2895b7abb3", "FR_e9cb25f7-c8ed-4c87-8722-362270fa5980",
            "FR_e733f347-4828-4dc6-8a37-3a6e3cacec7c", "FR_eefb5648-69e2-4c50-b519-36bb5367d971", "FR_1f645423-1135-4e85-b62a-2385f6b01a02",
            "FR_6be57a73-3728-49ea-851b-e5679d53c233", "FR_1db2d9be-3233-4d33-94dd-cec0ef4b3710", "FR_059a7fbf-3ed0-4795-a199-4cbeb76c699c",
            "FR_46084068-94f8-4040-b3ce-6af284ce0c9a", "FR_e2b6781f-ac32-4b26-bf47-6aa722f8af40", "FR_bc98bef8-7beb-48dd-92e0-1a8520704862",
            "FR_0876078f-3f14-4291-802f-ebc3e0f8e010", "FR_267a99e3-dc4e-49ad-839f-1c90e7b9f098", "FR_29b4eec3-0267-442b-a558-27c15d314254",
            "FR_33974e13-1d46-449c-aa2f-bf73eae36a41", "FR_83c2615a-8709-41ed-9d82-0d4501661983", "FR_c19b35ca-fd24-41cb-9558-8c64e37cef02",
            "FR_68d28881-328f-488e-bc9f-b8b53f6337a0", "FR_3bae3f36-e9bb-4d5e-a040-a25b6f63d6c8", "FR_48f85ec1-73c9-4ae2-9ab9-1084c220ad6e",
            "FR_bf52f6b3-845d-4c71-b1bd-f6f125df93f4", "FR_a5eb05f3-247c-4181-9803-796d34b798b3", "FR_96755801-57f6-4656-9462-4d444963e720",
            "FR_898170bd-adde-4a9a-804a-cbd2c12e155d", "FR_fa191836-c241-4129-9344-16964ff3fa9a", "FR_ab5b0239-5629-48a8-8550-eb241938afb2",
            "FR_872d901c-e89c-4618-b555-58d18708f3f1", "FR_193abab8-04cb-4d13-9798-403ed55bd40e", "FR_8aa2ddb9-e528-4d82-ab26-c5676e16107a",
            "FR_6af66201-cb0f-44e5-ba88-6778418e50bc", "FR_cf2f95aa-f1c6-484e-a9f0-58bb02de8342", "FR_451c3571-7635-4017-9489-9adf74c0b557",
            "FR_0ed76ff2-b30d-48b5-8f6c-8511ae9cea25", "FR_f9f1b628-46e7-45ab-bdaa-58fb3a997bb4", "FR_55fa02a0-ac84-450d-9325-6c56f1086374",
            "FR_64500685-a4ea-4662-9635-814674caae17", "FR_3e651a17-6159-4bbc-8559-09bbdc461ee6", "FR_8cb47b50-0b21-4130-aa6b-a57719c897f5",
            "FR_8e4a0527-4491-4856-ae8a-8357f1e491c3", "FR_73aa4fb8-18f9-4853-9517-bd513582f41f", "FR_14126988-1dd6-4c4e-925c-fb5bd6c5ee9f",
            "FR_89ca70a8-dcba-4512-97c2-ee0c14104b2c", "FR_9c2da2c2-f826-4ac7-ba83-926f0f0f8154", "FR_a1e53af5-3293-49aa-98d1-9b3ca5bad527",
            "FR_e386ec1a-a3cb-4e30-b2f5-4910aa619576", "FR_37ac7f88-195b-447f-bb66-a44cfa5a7b9e", "FR_858e7a08-f158-45f7-bb1d-67e8af911b0f",
            "FR_d8f0490f-a440-498e-a80b-c2424acc35be", "FR_7f23f464-b67c-4a95-928e-7dbcf70f24a5", "FR_fe129fc5-6c61-448a-ba6c-aa89efbf3def",
            "FR_20396f3f-2bcb-4999-befe-5da3e254c7f0", "FR_7106a64d-99c4-4836-b8be-0f11a85957c3", "FR_a215a021-0643-47c8-960e-dabf54572181",
            "FR_eaab5a5c-7ec8-438e-8234-bbed1876c554", "FR_99e66119-7abb-4713-828e-a8f806f497a4", "FR_d2b3e708-6e45-4f44-8a17-2831ebbabf7b",
            "FR_0e8a706b-7a82-4ecb-9912-712cc48f0067", "FR_d48f2c6f-b60d-4d0a-ba16-8979b4c930be", "FR_8aaabe0b-3a26-460a-869d-90718b0b71be",
            "FR_1a2da06e-3727-434d-b738-7fc3420443af", "FR_4546e03f-aae8-4a17-9fa2-e1c199cb1108", "FR_a8c1d6ef-2e77-4669-861f-38f0c40e3abe",
            "FR_daa692d6-7f0b-4e8a-82bd-a629aeca4285", "FR_93b739dd-e5d1-48f1-85dd-ad2a19017acb", "FR_d293ed85-7d88-458a-ad6b-17389c573e4b",
            "FR_2886d745-a334-4a25-b205-aa1311295ed4", "FR_266657f3-2796-47ae-928b-bdf8c10c2ae2", "FR_4c8e2e94-73d7-495c-a6b8-2a93c712908e",
            "FR_fd39fe36-bcfb-4ec2-8375-507f820532bd", "FR_b9a51fe8-bf23-4ea2-be24-f05b3f068eb0", "FR_5c55fd27-8a43-4bb8-bbaf-3a7e04013d3d",
            "FR_a8ec55be-3a75-47d4-a07a-4c17ac6823b5", "FR_28f463ed-2085-4357-a505-a1eccc2abee2", "FR_5c9d0def-aa2e-4fe9-a5c2-b53b7916ef42",
            "FR_55dbea76-6f88-4655-a232-0eeda2f56c1b", "FR_3107bc53-09ea-48fd-9d79-71fb1e7dc025", "FR_fa16f797-3efd-444a-af0e-da8e4ec6fb7d",
            "FR_88cd11dc-6b5d-4eea-93b1-b57901440c88", "FR_4850f5e0-3d0f-439c-bd6a-96ac3e2abf35", "FR_32e70601-71f6-4d2c-a2de-2a94ecde9922",
            "FR_1d284510-c436-43aa-a6c8-a73a94b6ffda", "FR_76757443-9c02-4c36-bdc9-eaee099e93ca", "FR_297e456f-03dc-493c-9297-379b1aba63bb",
            "FR_6ba141fa-e31f-4071-8552-bd46df50eb6e", "FR_3d7a6cf4-9f42-4b27-bf8f-8a121f1c91f9", "FR_c197431b-f05e-43e7-916b-79ffe885912a",
            "FR_153c6c1b-b68d-495b-8689-9d24870f08ef", "FR_050ca26b-5e1b-4924-af2c-508fb479a296", "FR_b44e5fb0-7989-45d7-a138-570275ddc15b",
            "FR_3875ac9f-2f5d-45d1-b624-bece6c25deef", "FR_f0cab563-0b0b-48d4-a1f7-80da59faa97c", "FR_fc342b49-2138-4c4c-bb5c-7a8146483c16",
            "FR_884e5547-75b0-4354-b58c-db5c4d2c1755", "FR_24d6dcfd-65e9-4537-be79-b37c5ad1f447", "FR_486aba50-811d-4a1a-96bc-2d371939992d",
            "FR_19afaf80-1216-488f-b19a-8d14768608a2", "FR_f3f574b7-6628-4c2a-9ecf-054bf8411d49", "FR_2702fb3a-d265-4f92-bc97-ae168caaeb47",
            "FR_29f870cc-239f-49a5-a8e7-a36df5855ef4", "FR_28817974-87ca-4ed1-83e6-2b84a19361da", "FR_bb20eb78-da2f-47e4-bb5f-2eaf76e60147",
            "FR_418ed44b-1716-44b3-95de-1467d36b15d8", "FR_49863616-a4a9-4ce9-99b9-235f54372402", "FR_0332ceb8-5a6c-431c-b420-0c91558628f9",
            "FR_35073194-34c1-431f-add3-681d2f3d7711", "FR_394e8a5a-1a5b-4854-a530-62f1101e786a", "FR_611b87e0-6d27-49fe-ad61-0615734a4b13",
            "FR_7e3468e2-ab7b-4d55-af69-af8c7792e0d2", "FR_0c4ceae1-3102-4e7d-98b3-8b2c4b3236ce", "FR_f0d467e0-9f0a-4f35-8c8f-14b2fc64ab89",
            "FR_9b79311a-d0d0-4bdd-837a-bee3bb32d963", "FR_b54e9a42-39fb-44cb-a434-ca7a097d1cdc", "FR_e1c53935-586f-444f-8a8c-3a289fb6a5c3",
            "FR_af811e72-a16f-4eb5-9b31-9ff75112a868", "FR_6dba2cef-c7e8-489f-8e89-aca16109f986", "FR_d95d5df8-6111-4691-976d-6f0db69261cf",
            "FR_66a910ef-cfed-40bd-9b65-9ab20c187613", "FR_3f565a26-d6f0-46ca-ac94-723b20170857", "FR_cdfd5084-064e-4c43-9e8d-2aac4c9ed1d6",
            "FR_d2d849b8-d4fb-48f8-8d2b-0d12c4e25502", "FR_dd39fce8-361d-404b-b5e6-fa9561de0568", "FR_4dc67f5f-50b2-4043-9194-e70bf77b8d75",
            "FR_e8cc420e-b1fb-4f67-a826-f492a3e02b8e", "FR_52420925-118f-4677-a937-654986314d25", "FR_0e2ddcf4-f67f-4356-ba09-8770d18d7b66",
            "FR_ac3dbfc8-4673-4ac5-aee4-c2d172db018f", "FR_e1066c15-1c74-41ea-943d-56ad8764f62d", "FR_b19090be-129e-4d33-bb6c-cc5453dbbd81",
            "FR_767a0540-fccc-4b75-a88a-8c3d97cb421e", "FR_46fdda4b-bb26-4135-9356-5ff22967f79a", "FR_503f6170-53fe-486b-952f-1f7bab8713bc",
            "FR_98f64e94-78d2-40b1-bb6a-0506a406ad5f", "FR_a7e6a25a-27a3-4603-9ce5-0d671453fcb3", "FR_84558984-2208-4506-8a0a-05594c6289f5",
            "FR_e74a5020-ea28-4af1-bdfd-e1aae44b2779", "FR_a6a2a20a-802c-47be-85ae-a431fe85e54a", "FR_dc86d1ae-e383-4562-985c-902bf7540a07",
            "FR_8a8c2410-1f90-4087-a35b-3c115c493b0c", "FR_07f5a77f-dd40-486a-8edb-d4c0a2661741", "FR_538debdb-1bb6-4ca1-8d6c-4efe5d2622f9",
            "FR_e089ef6a-824e-4967-9486-50f96736d228", "FR_d79187ca-9ad9-469b-ac74-f9741181fc51", "FR_fc6f0e33-1830-4128-8a35-c877e854e07f",
            "FR_b559d2ff-8678-4e4d-bfa1-173dd08050cd", "FR_9d743bb0-1ddf-404e-9d5f-16e0fc8ec7f9", "FR_d5e9c1ac-0d7e-4ff3-b8af-5b62d210d972",
            "FR_534100ed-3fb7-4422-b022-72c1309bbe68", "FR_4eee78ee-3ceb-47ab-84bc-7953890fb80b", "FR_1badbf94-23b5-497e-b130-f2403824b1cd",
            "FR_3cbcea60-76a1-487c-9591-8c5990d92da6", "FR_c328f63e-a34a-46ed-9faf-3ee4cdca3a52", "FR_e83f4962-c582-4327-aa36-3381f6179dfa",
            "FR_f3f11218-8b4b-40e7-80f6-f9f5d343c5f4", "FR_20135c4a-b37f-4fef-81ce-1fa4606bf62b", "FR_87be9b9e-6154-4978-a9b3-6667ae8c9df9",
            "FR_21758bb0-47b0-4c18-9a8e-6d2f996deca8", "FR_bb6b089b-d9df-42af-9037-7596d90eb78a", "FR_55e79122-e45e-48e8-a2db-bf691c1f3ce8",
            "FR_3d6b6557-2f15-4fea-bceb-bf2c32597ecd", "FR_2f7598cc-ad5d-42fc-b551-92bd53ee7cc1", "FR_1c127c45-f6a3-49bf-8cc6-2f4aced201cd",
            "FR_4e472d3a-3226-488f-a073-0eee3f7619c8", "FR_ce546f5b-c81d-4751-bb15-78c052560440", "FR_e7fa4002-206d-4853-b45e-9ed345618ce8",
            "FR_f610e1a5-7c23-4892-8ec5-13e1f2432510", "FR_1a971c95-2250-4fb9-9015-47b26e77e655", "FR_a43ae315-ebff-4400-ac64-3022476364fa",
            "FR_ad8f864d-886e-4894-8446-2b6e02dd231a", "FR_4d3e9617-ad58-4e3b-822b-28a63e0dd1fb", "FR_a8428c39-f041-42aa-81f1-13bb3cf886e5",
            "FR_a3e8f646-57e7-4600-bed4-7cf7b4b54d8e", "FR_0adce326-6691-409e-824f-46250e893f02", "FR_0fbe3ce9-2576-4670-93c0-bba6b389707f",
            "FR_a6c9abc1-3e0a-4727-9c44-74ee46b4df49", "FR_c6596277-3e8c-4b60-a889-b4a2948ad301", "FR_38040e6e-e7bc-46a5-85eb-82580f5ad0ad",
            "FR_877d73e1-5216-47e6-bffe-7d127c4d0db4", "FR_f461c9b0-701c-4083-9b20-422a6a31292a", "FR_10d905c3-3dcb-4052-8bd9-cfbeba5d8e23",
            "FR_a3f2856d-860b-48a8-844e-c99c3e9d1413", "FR_99846b4c-58f4-4bdc-991c-64f281ad50f7", "FR_d0a11a7b-5dd1-4307-84df-87c728b7f241",
            "FR_9ae17ac0-4955-43ea-a332-553b538bfc2a", "FR_2a266571-e4f6-473b-a260-207bb052eeba", "FR_82d854ca-ac02-45e4-a4a8-55e11be3f9a3",
            "FR_abfa1a3b-67bd-4e57-9b8b-e82615659af3", "FR_b2b41189-da00-471a-810b-f83b4e473ce5", "FR_5f94e6c9-e26e-46d7-9d31-52c7d4461331",
            "FR_8f63649e-a155-466d-ba98-5e5f4088f6c1", "FR_fbdbbd44-f9da-4662-a8a3-fda44abbb79b", "FR_98ca810a-56e7-4cde-a67a-0ac722342ed1",
            "FR_8503ee93-839e-4a79-9b49-8bcc2549c371", "FR_104ffb88-c637-4acb-8c78-548a9f66c02e", "FR_f28b075a-2252-4e6f-b912-dc5789d7eb4a",
            "FR_c942ef56-8400-40dd-81f2-0b6d05f8c9b2", "FR_671c594c-121a-44d0-b89a-ef8c02ed61ea", "FR_047f2d6e-6147-4534-858a-0bb95bcb92d3",
            "FR_f1097edc-d18f-4f3a-8e41-71f7719ee1c0", "FR_378d6c8c-6723-4362-ab5a-77497bb4661e", "FR_006c08bc-c847-47ee-83eb-cd8cff1e2a53",
            "FR_c5bd83af-d3cb-4f7e-a15b-3e956e4a4a81", "FR_5e166488-c212-4994-b274-ea6a9b394b9c", "FR_9a33ff4e-6a4a-4f77-aa41-1a8c0bfe1ba7",
            "FR_f9646e47-50be-4d09-b8b8-77ec45554939", "FR_f9fbb533-023e-4b4d-ac5b-cff254f8fda1", "FR_a9c16f37-ce90-42e3-ab8f-e14a1a82572b",
            "FR_7db191ae-5e3d-41ce-8da6-9c372658495d", "FR_f2c01531-1796-4b16-bbe3-2f336df0f86f", "FR_9d7b993a-e89d-4418-80e1-7e5c1c4d2cc9",
            "FR_16eda40b-083f-456e-ae49-b34a3be51437", "FR_542aabf1-ddce-4bde-8d2b-b75dbbb6f20d", "FR_577e8b0d-c08d-4817-b699-baeb1d72681c",
            "FR_8f11c5da-6cfd-4a30-b326-2b67d8a8e7f3", "FR_01aaa48c-db72-48dc-8b49-baf8b021f672", "FR_58c26a09-26f7-4db8-8d69-ed2ff4f4e972",
            "FR_c04c12a7-329a-4521-b1a3-48d1d1f3c6d6", "FR_2c2f4519-2dd9-4b1a-b5fd-aeb00840f89f", "FR_6121cb52-d10c-4dbb-9b4b-40344eed8f1b",
            "FR_a7386c1b-b16e-4143-a039-e25121f00aca", "FR_91d37788-7edf-47b4-89ff-52c9c8f3dfb8", "FR_5a116052-a9b8-489d-ae16-f584b89efe70",
            "FR_ace70db8-9a80-4b68-8ba3-752394e1131e", "FR_63ac77f8-ef30-466e-b12a-0e871f97b106", "FR_621fbdc7-1215-4b98-917b-d48ea305dcb2",
            "FR_59ccbba8-8d77-431c-b1fb-3217aee1314d", "FR_47592b38-4e49-4108-ab50-5b2aa7a1d6aa", "FR_97da3e30-0930-4854-854e-6e4fd776240c",
            "FR_2f2ee634-a886-4c9e-ab99-f0aad090c1b2", "FR_4214e93f-79ce-47bd-852b-b7a7c8b42a28", "FR_425eb2be-8c35-467c-ad9b-9404172b7d2c",
            "FR_c9842c39-3991-4bbe-9dc9-5213d9a475b5", "FR_497f8a7e-b3e2-4ad8-9ddf-ca23495d498c", "FR_7f01a869-20a8-42d5-b106-6125c4d70cc4",
            "FR_30345137-93a0-4ea6-a594-67cee02c0ad7", "FR_7d0dc4c9-4d73-4baf-ae0d-a01e40357098", "FR_2dfc518d-aa2e-4c88-ba7b-7edd09f53cd4",
            "FR_b5939ade-8660-4e66-b610-7fa812e290b6", "FR_7db1edfd-c761-4c1b-867b-ef589055adb2", "FR_c48bacce-b0f0-44b8-a975-2bf20d310573",
            "FR_2cefbb20-6f65-44e1-b329-b25c76f4e56b", "FR_d8008473-f915-48a2-a996-cbf6088fe7fb", "FR_7a075928-1e96-4132-9cf4-125ae752cab5",
            "FR_89fce81a-f753-4c1d-b47f-0f458e9cfd00", "FR_eebd9b20-6974-43ed-ae3a-18b97bf8b38a", "FR_63a68533-d31f-416e-be32-53137d6e0ebb",
            "FR_3cb1e5d9-5e68-4ad9-9da8-b4a7972ad6ec", "FR_15bfe1ad-21fb-49fe-b6e6-819cda2a3d8c", "FR_f1e76a73-2eb8-4d77-9811-8927a591d5c6",
            "FR_0469573a-5cbe-4fd7-a537-0ac75b20b2d5", "FR_eebec45b-e293-4f4b-91e1-747395b29a3c", "FR_515422e5-27f4-4e4a-84d3-e2bb8cfdba0a",
            "FR_1ce5d7ac-f884-40cc-98c1-605fd4599f8e", "FR_ffcb6d13-b58f-4abf-aba4-5c1ea088a98d", "FR_21fa5816-d8f6-4cb9-b24b-5ac9cec94941",
            "FR_be52a308-8e35-4422-ab0c-672281841ec6", "FR_2a05a0ad-c749-4744-a81b-287594672d4c", "FR_9ab6b3d6-82b7-4ea5-9c61-1fce32245568",
            "FR_1e4e8ff4-379c-43cd-ad7a-8eba26f0062e", "FR_32be1075-b30b-4047-b041-72240cb6ac3b", "FR_f9a1727b-93ba-41ac-9582-fe0118b31cdb",
            "FR_185bc517-8c49-4dce-9f17-fdf3abc99c07", "FR_22d8e231-7932-4159-90fc-255ef1f37cdc", "FR_c009c602-5398-4c47-b685-19c697f2698e",
            "FR_a1f8e4f9-94a2-4b34-9b95-b7e262d3ac33", "FR_240cc842-6a24-451e-989d-45e9606326ec", "FR_a668ff69-56c0-40fa-99f6-7cb11c855750",
            "FR_85bf7bb1-145e-4929-a620-4856534889b8", "FR_3b718216-9fcb-4036-a48b-317757a69f58", "FR_459c2c8c-c286-4969-a76d-0caa08f12d7d",
            "FR_7ece7952-9036-4f71-ad1a-210b877c677f", "FR_626a93f0-653d-4295-aff1-2c93ba7df01b", "FR_e3feb45a-ce3e-4693-97f5-d6fe1d4cf60c",
            "FR_945270f6-ef9d-4430-8a07-697311fd6f16", "FR_a547ed0d-9264-4e40-a992-6d9f20e97deb", "FR_dd525ea9-2a90-4d20-9470-3d435567d668",
            "FR_464c9dae-6476-4256-ad29-ca3d4184448f", "FR_1c8e800c-c4f0-4aa5-b8d7-36f34405eb5b", "FR_ab169c16-4049-4094-9e82-83f46425aaa1",
            "FR_1ef7b7fe-0687-482d-95b0-91a6e281711f", "FR_615d2fda-7a48-4af7-9ba9-e1c374c24170", "FR_1f1c7106-ad00-46e8-a938-13b4f20b0e28",
            "FR_1e5deff2-e002-47c5-948c-bfa09571cb81", "FR_3379c192-6183-4035-af34-ebd875e7fc7d", "FR_35c2fe19-c8de-425a-b523-e52d457c28d7",
            "FR_30b5fbac-e790-4400-807c-47e799bebdc8", "FR_6a5f1794-42b5-4369-af67-ef74d4645928", "FR_45e50966-2b28-4d29-b5f9-3f47de8ef6ce",
            "FR_d341ed3b-7871-4f10-9f19-306c156278b1", "FR_852649c1-ba0c-48fa-8871-c29d7bb0eb74", "FR_160beb68-29e2-43b0-a273-dc8a5baefa01",
            "FR_9695a3dd-572a-4313-bccc-2c90720aae45", "FR_c1d118c2-f7b5-48c9-8f03-6b98e43c2e73", "FR_3d32dda0-ebf3-4f65-bc63-40fdba4b1cea",
            "FR_87c5b0eb-a70c-4161-9fae-5b4ff9a26173", "FR_90a29827-2db1-4ee6-83fc-3bf040c3495a", "FR_b171c5e1-6bbe-4887-974f-43ddbced27a8",
            "FR_24b39ce1-0765-4e39-84a7-e8517a9ca962", "FR_d442ff5d-9bb3-4d70-a65d-761169ce0f25", "FR_613c7442-c63c-4a2b-8e39-2f67763ee57e",
            "FR_064cedf1-be1b-4c62-a307-f19715fb1a69", "FR_67e55455-5a39-4b6d-936d-0700551167b7", "FR_522e451c-5663-4e67-8cb4-89df670d3a1d",
            "FR_741430e8-edd7-4f0d-a3c9-15a022f295dc", "FR_964040f1-75d5-42cd-b4e8-a3e32ee8933f", "FR_629770d2-43b8-4108-b2e2-af45b8e887a2",
            "FR_3e99d3e1-3b00-4866-a9a1-13a4b0bbfac2", "FR_726455bf-4f1c-4e2e-a741-198d247b038c", "FR_f1734808-0c71-4c52-acbe-279a6fb0c5fd",
            "FR_81376abf-3953-4d74-bd1a-0b82ad275249", "FR_0e503fac-9688-46ff-bd4a-e3ead339c40f", "FR_eee2fb4e-7ef9-47aa-911b-b780c0b51eff",
            "FR_f4b9ac12-dccd-44c2-8a1a-368bd7cac276", "FR_2c54a241-ae9f-48cd-908f-9915ceeae032", "FR_283703d3-d4cb-4273-8dee-2d968b280eca",
            "FR_fdfe0db0-0cae-46ae-9662-0df46415eef7", "FR_7073b316-f8f8-45f6-a451-e56107d9db35", "FR_07c837d6-35a0-4ce4-9863-cb07ee4fcbe6",
            "FR_e0f75415-c018-4496-95c1-db8b072de883", "FR_8771749d-2d80-4a55-9b9d-68601ef31cb0", "FR_f4f4d90d-349b-42f5-b008-6005816789a9",
            "FR_b9da6b95-a2eb-493b-b4fa-cfa0fd63367a", "FR_e88f415c-b2b5-49ba-b6fe-62e9d667bed4", "FR_37e77a99-1ae3-4a28-a68c-f8e331d67c9c",
            "FR_78151354-eb2b-48b6-86af-8c765b981f10", "FR_741bebb5-3ced-4bbf-bcf4-16d838a13259", "FR_14395deb-ce77-4553-be6f-49e35fbbbf73",
            "FR_9b4016f3-a3f0-4701-b2ff-421f9c289e2f", "FR_66c13ec5-806e-4bf2-a85c-6511b1231314", "FR_e4f20cab-7ec6-44e6-b12c-a66324150405",
            "FR_35a6aa7b-2e5d-46ef-bd92-65e4a007802c", "FR_a6223e9d-b3ac-4b44-8b2d-51ddd34e0929", "FR_bb230b49-ad38-4b20-8038-97c5a10d091a",
            "FR_0e8765c9-0d3c-44ee-a178-5df79b3931be", "FR_cd9a1717-1efa-4692-8908-021ae6af5362", "FR_6b400f59-c34c-43d7-b6a7-96658e380d7c",
            "FR_b99a3610-ca52-4479-883a-fe85a0577bf6", "FR_812e23fb-980d-47c0-87d2-5ac206efa825", "FR_1a2851ed-b380-4325-953d-5fad99cb0f85",
            "FR_03b669b3-c287-43d9-95b4-9894e607c12f", "FR_44ba4e4d-7ba6-431f-96f6-ddcfc37ec37e", "FR_b6e9ee48-d0a9-41a4-b449-01d4ff449917",
            "FR_c4307037-fe73-4661-92d8-a47a4e6479cb", "FR_d84c2a59-79e7-4a05-bea2-d6c84a780461", "FR_796ab1ae-6c6f-48af-bc4b-b047fc51bfb0",
            "FR_1de03747-3da2-46f9-accf-489e60f4e1a9", "FR_34ef8180-0c5d-4180-92aa-31fd15f54bb0", "FR_23e24ae7-3348-4eeb-8298-8c2583c74f9b",
            "FR_788c4cdd-c450-491a-a35b-b5de73112b74", "FR_5281815d-4568-4276-ba77-eba8c7f77a8c", "FR_9623cee4-0003-48f2-b9e0-d0b30c05e9d8",
            "FR_0828e567-c07e-4c47-a4e1-12af679d92c2", "FR_2a23a462-7f6b-4110-9973-d3247810bcc2", "FR_e3d75150-3ec4-45fe-abb4-90328fdddceb",
            "FR_a906a4e3-d369-442b-a9f7-d241c440459f", "FR_5e551351-c8a8-4bce-85dd-7be842831c58", "FR_bcd894f3-d229-45aa-9175-9f810f913bf3",
            "FR_fffab0ea-c39f-45f9-b1b6-a280c44f3a4c", "FR_14dafa6f-a879-4b68-b333-168225e1d20b", "FR_0ee2497e-3059-4a9b-a3a1-9fb9e3eede51",
            "FR_e5861ba4-dc06-43c7-a8db-c5dc36617209", "FR_801ced53-3de6-4940-9a42-a103dcbde488", "FR_c32d82ea-fdcf-40b5-8ee1-b18dd7fc51ed",
            "FR_fa8b3373-8c19-400c-b441-ef58e7681de4", "FR_070ffd69-72e6-4a98-b446-bc85b5d60f65", "FR_eef483ad-1e07-4873-b0e8-888b4f55c15d",
            "FR_276f2eda-5cae-40d1-9ff2-acff4ce547f0", "FR_b9402e43-7864-4bcc-9ccd-693effbfcc30", "FR_a9fc39d8-f3d2-4a68-9ce4-7dca1f6f43ad",
            "FR_cda18147-ddcc-49b8-bd65-dc3e1dd0beb0", "FR_e61953c0-981c-4120-ab05-cde94135c788", "FR_551dc849-2fd3-454d-a182-3074be551660",
            "FR_a42d0e59-1b81-485e-925b-f7de6ad76e2e", "FR_50731af8-c2ba-4ca9-b4fb-3ad7f0e241d2", "FR_21cfbbb7-c191-45a1-a59a-229d9ee47341",
            "FR_8ee32fa5-482b-4807-a687-f28e5710fd22", "FR_bfcaaa8b-b05a-4de4-b2e7-780d84c02abb", "FR_7a90e106-4e7b-494b-9707-c6b726892fec",
            "FR_f377163c-8d3a-4bf8-813a-35cafa182f60", "FR_9731f6d3-a8b7-4589-8eaf-f9121cbbc019", "FR_2e4aecf1-b7c7-4a59-9f01-a4e4671fedc8",
            "FR_60b8b78b-e292-4009-8eff-8c19fd484c1b", "FR_05b93751-e7dc-43b2-8c55-0b4b9b1f5e0c", "FR_da607c3c-bf95-4db8-b0ae-1c570cf9613c",
            "FR_82a311bf-c5ae-4bd8-93a1-c5d46fce4096", "FR_c4dab70b-875f-4e45-b003-0721d3b461d8", "FR_9cebab0d-78af-4793-a5eb-157314dfe72a",
            "FR_e5d96c6b-5288-4ff4-b687-451cad6af27e", "FR_40810875-44ca-4353-aa52-c326ff3326e8", "FR_0801d57f-6cdc-42de-a4f0-d6d492bad21f",
            "FR_ff1ab00f-5754-4aff-8592-bad383eed097", "FR_e445d4ac-473b-4306-84f2-4c5fc8a227d8", "FR_bff5d86f-d43f-4c62-afa1-568b6411a28e",
            "FR_22b20a32-39b2-4929-bf01-f8c2ba64580a", "FR_4a635667-3854-4744-b519-97ac3e83068e", "FR_97a1ee01-f65c-4d09-9f3b-a573024313d0",
            "FR_cb76136f-a415-4c82-8a35-030b87fe81f0", "FR_ae35240f-13f2-45d7-a947-80bdfa244776", "FR_776dc258-36bb-4d59-a72b-125a63a5d260",
            "FR_b05b4e15-641b-4f7e-93fc-4626947ebeca", "FR_4924c99e-818f-46d9-a8ef-2ec34ca2bda2", "FR_d3104a6a-93d4-4fbd-97c7-baa2c08d5846",
            "FR_4540df87-d7e1-44a7-9edb-efff8fdc1904", "FR_948a2674-fd51-407d-80f6-3128bae09c8c", "FR_5956c1d6-2acd-4a95-a8cc-676657680776",
            "FR_d5f5b0f4-2548-4a68-bdbe-655156e21ef7", "FR_75a691b8-f9bb-49bd-9419-dbd1749e54a8", "FR_f66abdf5-94bf-4490-8495-5ebf5bd8bf14",
            "FR_f93ebe22-4458-4f5b-90cf-b376d166f376", "FR_67df9af1-e879-47fc-84e1-fc385af049ba", "FR_7489cc39-3ae0-424a-88b9-cbf5e3b69d03",
            "FR_e7ff2576-8fcf-4501-a212-b2f4ec6d6b37", "FR_557e6c88-45db-4e3c-9fe1-1e8653506c88", "FR_d5cbec0c-4522-4083-ab81-a54cf7a7be61",
            "FR_08ea8d48-8842-480e-b955-9f02db65c9aa", "FR_9b890730-452c-43d3-95e5-6500b50491e5", "FR_13852553-7114-44f3-b37b-92fa3f70e37d",
            "FR_9c7fa8b3-6af4-45d8-a04f-8782f077a236", "FR_2f668380-191d-42df-abef-710c38830099", "FR_1e12e106-ff4e-4f5f-b211-4d79caee664b",
            "FR_69efec37-df22-41c0-b750-409fcb8e75c2", "FR_de358d26-77fa-4e0d-8385-0925df362587", "FR_bfe29058-8546-49d6-ae35-5e5a6ce3acc5",
            "FR_c0b79379-93de-4eab-869b-9ac36fcc90e8", "FR_c46f5ba3-0031-4a13-9ad2-08f6f86b45a6", "FR_c0555397-2f20-4c65-b250-be9a1eb72161",
            "FR_6ef9df95-b073-43d6-8e44-59892d2497ef", "FR_349bb5d1-3d39-47fe-b0c1-efc927b1dd24", "FR_e9a53ab3-ce8d-44d3-a5ca-24c6bc9e75f3",
            "FR_cf6b37f7-2c7e-4832-bdd0-bd5944054542", "FR_01bd5133-c722-4554-b185-7a317cf58873", "FR_b76f668d-e2dc-4488-ab75-0e40d324a113",
            "FR_ec0b073d-ac4a-4523-b003-45f0dd158aed", "FR_6e621b60-684f-46ab-8285-bc8242156e24", "FR_f231aab0-9947-494d-a342-e8999b3bc748",
            "FR_1e81acd2-c6e9-4108-aece-a39e7eff3d67", "FR_69711bcd-8b2e-4e8a-9abe-76e1f2ce4ef0", "FR_f9072310-e036-4309-9973-3e534dac9317",
            "FR_59ce6072-18e7-4935-bd76-f68c16407cf5", "FR_4d2260f4-76f0-40a3-b765-095ac3337264", "FR_2768b091-0d24-460e-baf3-4789823edd4a",
            "FR_d5499e76-ffcf-4b35-9c26-2acc6cfd1cc7", "FR_b636b385-93f0-4b12-8b0e-2a4fd369c31c", "FR_984d6b9a-dfb6-4c69-ab3b-fd5b6bca2c87",
            "FR_14e0566d-8a32-46cd-b72b-85b8096ee8b5", "FR_5911bd8b-d5d8-4079-8948-32f03cdf1c7f", "FR_54004c46-842b-40aa-a94e-08cf5a826190",
            "FR_3f1d4737-28f5-4323-aa97-4dfdc0b48b5f", "FR_b59a60ac-5cf3-4a52-bfb7-8691b7772de1", "FR_36d11002-5ab3-4f69-bd93-0a93f3a3440a",
            "FR_fb658ec7-6f79-412b-963c-7dfb7fded64c", "FR_4025cc4b-cd9a-488e-bdce-20bf7d6efc7b", "FR_f453b5cc-eefe-4d9c-b4a5-52757747818c",
            "FR_4219154e-f4ff-4998-8514-1a864bdb873e", "FR_9ea1b694-8295-42ee-b645-2959a28105ac", "FR_e2eca2ae-fb47-46ab-bdb2-18412deaceae",
            "FR_092c61dc-482d-4d1c-ad38-620d0f2bc280", "FR_5963d63c-6f11-48ca-9872-3c5a49e044de", "FR_1560ac77-15a5-48c1-9f2e-461dd2f7ee11",
            "FR_1e250825-18d2-4cba-99f5-5e55a0a0791e", "FR_2b714599-d82f-4692-83fc-0b0d0ab81672", "FR_099d8409-4f15-447f-b27b-5d475456d0a2",
            "FR_eee54057-6117-4cac-a274-879b1087ad9a", "FR_3fe9db6a-68b9-44ec-bdcb-0fea53f11bca", "FR_9bd06796-52a5-44f0-9a5b-94adf1acaf79",
            "FR_527b5cd5-807b-4011-85f0-d3d2a9fbc0e4", "FR_fcb6bcca-e3b6-47df-a544-483ac6eb16d5", "FR_89fbcff5-97f8-4358-9452-65e6002a9462",
            "FR_528f3cbc-eb17-4d8c-a4e0-e1ff2cdd2a4a", "FR_7074d325-e4b1-44b2-b294-942350437c0f", "FR_8e254d91-29e6-449a-a3b5-e4cfb42d02c1",
            "FR_8fdfb0ce-126f-476f-93f7-c36462c2ed6a", "FR_ed68e08b-d163-432e-b5bb-a7b980dc5c77", "FR_80bcd699-85d1-4dd1-90a5-223468d79db4",
            "FR_9944d541-3032-4d5e-ac53-1432103b6242", "FR_3c91ec62-fb9a-438f-9fe7-cd700bf3a215", "FR_c5140f41-90b6-4da0-aa94-7c7401a65a38",
            "FR_42bc45f6-ee6e-4700-ae49-73db3598cbb2", "FR_4b19928b-c461-4989-a2aa-74ec5bd28be0", "FR_c2abd554-fa9e-416d-8cbf-0cd0c7a8aaf4",
            "FR_ab889808-0153-437a-ba4c-5866586a8147", "FR_d8e1a3c3-1451-40c3-8053-b9b958b89dc3", "FR_fd1a8804-9d86-431b-9356-5fc2b9e6d054",
            "FR_b7ff5264-80e7-4e5c-a6a4-659bd9bc4309", "FR_eae10aa0-85b7-432c-80d3-5b81b3583312", "FR_d4639f0b-f2e7-4ab8-8433-bb0d36bf8412",
            "FR_38ec530d-6ccf-4fcb-bbd6-68c994307ee5", "FR_c1b2458e-6ada-486f-a065-c77d4f686900", "FR_ee718ca4-b0c8-4ec4-b438-8d360f5ebc19",
            "FR_b88e9f14-bd44-45a1-bbbd-318e3e77e8aa", "FR_df4dcd37-eebf-47a9-8570-295abd386ac9", "FR_161c2c52-beca-4d55-ba3e-ad2642a58ac3",
            "FR_1f1bd438-b3d0-44d8-8c85-00c6c674de65", "FR_46dee55a-aec7-4f9f-b427-a79d9c92ae75", "FR_49929f4f-ba6f-43e4-9855-35e6312139d8",
            "FR_9501b251-4321-432f-b8cb-a23cd7657f4d", "FR_bec0d903-09b0-4f15-994a-2b98fefa8971", "FR_cb1e2b67-9ff5-46c0-b5ca-2ef50b76387c",
            "FR_666cea93-0402-4877-bf4f-2c727cfbadd7", "FR_7dd6ae97-5ad6-44e1-8aa2-31f54c66a730", "FR_3bf68531-e695-4742-ba41-8e5f98590626",
            "FR_224b4584-de9b-4e2a-a43d-246cecbc963a", "FR_b9b27ade-45e1-446c-9799-c505daa8dd83", "FR_b20e83b1-278a-4896-8d26-74d4e3b4a617",
            "FR_fb579feb-be4a-4b3c-9c55-79228d6733e9", "FR_853d82f1-5916-4f2d-ac62-dfabbed3a539", "FR_2d926d64-3e2b-4b95-a74e-421c9e123208",
            "FR_172822ed-57af-4be9-9130-74a658c6ed7f", "FR_dd15dbdc-5b33-4bf1-801c-a6e722a90fcf", "FR_a557fdce-cba9-47c8-ada0-2fbed237f398",
            "FR_ead4b4a4-ad12-46a6-9a5a-4812a8a7e1e5", "FR_0cdab73b-2c91-4767-9007-97bb6e926e78", "FR_9d4ccc07-8361-409b-bda9-272beb759f4b",
            "FR_771350e0-4060-4b91-bc99-7a4327c4e76e", "FR_62171443-328b-41c9-88ce-31c8ed896eb6", "FR_7f542a3d-8855-45d9-85e8-008ecc89a5a1",
            "FR_00ea6db7-726c-4223-b910-9f2c9de05379", "FR_dec243be-2fa9-48b6-8e9d-711f8e7cf0dc", "FR_fd62ab0f-3857-4c5c-9348-80e314898e40",
            "FR_215beaae-e6cf-4ec3-ab69-4c85809446bc", "FR_f29f0d66-7575-4102-8f61-eafd4e2659cb", "FR_f5644f8d-b0d3-4a91-8cee-b710f455e9a0",
            "FR_26a1aebe-6f05-4350-a354-a3c67a98a120", "FR_52fb5cbf-d890-46d7-a4c8-5cd4d85e1959", "FR_e677b400-3571-4cd9-971f-95a6bf8ed441",
            "FR_858bc07f-c4d8-4248-9124-f6107ca8a366", "FR_7a3c704b-80a0-4a92-a54c-a618bbf0c054", "FR_31e71239-22fc-4fd7-abb3-006ebcfc8e3b",
            "FR_9ba0ee71-24cc-4546-998e-8dc2ebe2dba3", "FR_356bafcb-4fa4-4c07-8dd6-7d2ad7de796c", "FR_96afd8b0-faa9-4332-b1ff-40b37d286956",
            "FR_bf4d7c63-1db5-451a-9b6a-9fcd67830631", "FR_6b80c2d2-61a2-4ada-9e7b-dd6e120102fc", "FR_d485d1cf-03d3-4b72-8622-8cbdb49ed98c",
            "FR_f7f88d52-7c8a-4b43-9565-0fd85bb9caff", "FR_7a21376e-a4c8-40b1-a80a-d339880f07c2", "FR_12843cbd-7ea2-48b9-891f-c575f2d157ef",
            "FR_ddaa4d02-90dc-431e-8cb5-a7a34188a9f8", "FR_d56b2a66-2378-41f7-b807-7f5050136a91", "FR_f85479f5-f54c-4db0-9fd1-59a00aa0a62f",
            "FR_4471260c-d4d8-4ba6-92dd-e3a019b2c7a2", "FR_1808c79a-652f-471f-9cf4-6c6023487e98", "FR_1822307e-3e6c-445b-a346-f1b8ad143103",
            "FR_311afa40-d992-4835-ada2-e69638109238", "FR_68aa1eb1-e3e2-4bb1-ac3d-5f3411225713", "FR_46b64073-e49f-4216-82b7-da030e86ab31",
            "FR_86ea92e7-de7a-4fb8-9c0c-eff27670d2e9", "FR_9bf145f5-dda2-48db-a309-a88646380359", "FR_b53ff515-4f3f-4c32-bcff-835e6d8a42ba",
            "FR_55cea6fb-d9e1-45e2-8992-b411a578a37c", "FR_f23f8416-d606-4bbb-b2ba-490a35fdff3e", "FR_aa102f02-4650-4a16-9693-c3f8700dbe0f",
            "FR_4d3d93c9-dc69-4c15-b848-35a03e1c65de", "FR_cbdbe460-1d93-4668-9787-494115ac07a8", "FR_578f93c1-00d4-4945-a28d-d2c806553395",
            "FR_10f3a8ad-1301-40cb-8950-243c328d63d9", "FR_a8b1bbbe-bf61-487e-88d6-af2ea5523e99", "FR_201e2c15-f459-4fc2-a2d8-a5386757ec6d",
            "FR_52ed0864-5f88-49d3-b983-0b5798274d5d", "FR_6def1c14-34e6-48b1-ac2e-1cdf7dfd56bb", "FR_ac3f1484-57d4-4403-97ac-9c5bdc14a5b1",
            "FR_59805c89-f180-4e91-b6a8-f3d213fa9f42", "FR_3eb74363-6d9e-4a23-8ad3-970d86274c9f", "FR_41f294d7-cfcc-41cb-a086-b04bacf3b57b",
            "FR_ee047565-5c38-4743-be6b-9fbc5522d780", "FR_d76778e3-7a83-4295-aab5-0aca521cf693", "FR_c1a55283-24c1-4685-a93f-37c9b28b90e8",
            "FR_ae363324-9578-4366-9a65-1d7243b84b69", "FR_97a89912-7a55-4f03-8b45-f8a757e82d1f", "FR_12ceab8a-0a31-4c73-822f-9c1c5d3cfb5f",
            "FR_9289916e-5607-4411-bb28-b2a5c2bf05ae", "FR_fd6df264-4a97-408f-9e8c-bb9bd79370d4", "FR_67f99def-aba5-44d7-95ee-12bfc03d2a2e",
            "FR_c4c66beb-013f-443f-b1f8-90672d0c7405", "FR_43fdbcbf-178b-4bae-a435-52229ca2424f", "FR_1d12b590-917c-4d25-bd47-8977974f6922",
            "FR_132211fe-1114-49df-94e5-ac6c392931fe", "FR_c9f05bfe-5675-44f4-b79a-95febff31952", "FR_a2d4d7fb-6d68-497d-99d8-d15a27cf0ea3",
            "FR_40923ca9-98ea-454f-8344-2405511985ee", "FR_cad188cc-8630-4708-a173-356b978ab937", "FR_532bed56-167d-4edd-b6a6-69780ce594ef",
            "FR_42f77f60-7bc0-46b0-8df3-da0412629a2b", "FR_43fee7d3-6947-43cb-bbd9-d380e95ece7e", "FR_7b619c18-39a9-44a1-8417-12fec5120929",
            "FR_eac1e333-95ee-4256-a738-f54cbe7a7c5e", "FR_ab5c179f-7240-49d8-9e11-7e126893bed7", "FR_f3fa4e73-97ff-4dfa-98ff-40c62631fd51",
            "FR_b67b35a1-59d1-495e-b794-72181ae73f4d", "FR_1627a31f-3947-491e-beb8-c5e76fcb233b", "FR_b43c3e94-26eb-46c9-8a5a-3fcec5587c57",
            "FR_9b35546f-4d79-41ac-817d-c90ab78ae518", "FR_d1bf3003-b984-4317-8ab5-952a6b6641a4", "FR_2906f5c8-9b0e-407e-becc-e24c5710151d",
            "FR_adb1a656-8f95-4fae-8e58-2806ec1e9c65", "FR_81d46e6c-8046-482e-a28a-5986cdffac9b"
        };
        this.random = new Random();
        this.sender = new MessageSenderGateway("StepChannel");
        this.props = new AMQP.BasicProperties.Builder()
                .correlationId("")
                .replyTo("")
                .build();
    }

    private void run(int vehicles) {
        if (vehicles > trackers.length) {
            vehicles = trackers.length;
        }

        simulations = new HashSet<>(vehicles);
        for (int i = 0; i < vehicles; i++) {
            String trackerId = trackers[i];
            Simulation simulation = new Simulation(trackerId, sender, props);
            simulations.add(simulation);
        }
        
        simulations.parallelStream().forEach((simulation) -> {
            double startLat = randomDouble(START_LAT, END_LAT);
            double startLong = randomDouble(START_LONG, END_LONG);
            double endLat = randomDouble(START_LAT, END_LAT);
            double endLong = randomDouble(START_LONG, END_LONG);
            RootObject obj = fetchRoutes(startLat, startLong, endLat, endLong);
            if (obj == null) {
                System.out.println("Failed to retrieve routes.");
            } else{
                String message = String.format("%s: Fetching routes.", simulation.getTrackerId());
                System.out.println(message);
            }
            simulation.initialize(obj);
        });

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (simulations.isEmpty()) {
                    timer.cancel();
                    System.out.println("Simulation complete.");
                }

                simulations.forEach((simulation) -> {
                    if (!simulation.step()) {
                        simulations.remove(simulation);
                    }
                });
            }
        }, 0, STEP_TIME * 1000);
    }
    
    private double randomDouble(double min, double max){
        return min + (max - min) * random.nextDouble();
    }

    private RootObject fetchRoutes(double startLat, double startLong, double endLat, double endLong) {
        try {
            String urlStr = String.format(Locale.ROOT, BASE_URL, startLat, startLong, endLat, endLong);

            URL url = new URL(urlStr);
            HttpURLConnection urlCon = (HttpURLConnection) url.openConnection();
            urlCon.setRequestMethod("GET");
            urlCon.setDoOutput(true);
            urlCon.setReadTimeout(15 * 1000);

            String output = getResponse(urlCon);
            RootObject obj = gson.fromJson(output, RootObject.class);
            return obj;
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    private String getResponse(HttpURLConnection urlCon) throws IOException {
        urlCon.connect();

        try ( BufferedReader reader = new BufferedReader(new InputStreamReader(urlCon.getInputStream()))) {
            StringBuilder stringBuilder = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append('\n');
            }
            return stringBuilder.toString();
        }
    }

}
