import Vue from "vue";
import * as lightning from "../sdk/lightning";
import Msgbus, {errorHandler} from "./Msgbus";
import JsonObjViewComponent from "./JsonObjView";

export default Vue.extend({
  template: `
        <v-container fluid>
          <v-card>
            <v-card-title>
              <span class="headline">Wallet</span>
            </v-card-title>
            <v-divider></v-divider>
            <json-object-view v-if="chainBalance" v-bind:object="chainBalance" title="Chain Balance"></json-object-view>
            <json-object-view v-if="channelBalance" v-bind:object="channelBalance" title="Channel Balance"></json-object-view>
            <v-card-actions></v-card-actions>
          </v-card>
          
          <v-card>
            <v-card-title>Invoices</v-card-title>
          
          <v-data-table
              :headers="headers"
              :items="invoices"
              class="elevation-1"
              :expand="expand"
              :pagination.sync="pagination"
              item-key="r_hash"
            >
              <template slot="headerCell" slot-scope="props">
                <v-tooltip bottom>
                <template v-slot:activator="{ on }">
                <span v-on="on">
                  {{ props.header.text }}
                </span>
              </template>
                <span>
                {{ props.header.text }}
                </span>
                </v-tooltip>
              </template>
              <template v-slot:items="props">
              <tr @click="props.expanded = !props.expanded">
                <td>{{ props.item.add_index }}</td>
                <td class="text-xs-right">{{ props.item.r_preimage}}</td>
                <td class="text-xs-right">{{ props.item.r_hash  }}</td>
                <td class="text-xs-right">{{ props.item.value }}</td>
                <td class="text-xs-right">{{ props.item.settled }}</td>
                <td class="text-xs-right">{{ props.item.expiry }}</td>
                <td class="text-xs-right">{{ props.item.state }}</td>
               </tr>
              </template>
              <template v-slot:expand="props">
                  <v-card flat>
                    <v-card-text>payment_request:{{ props.item.payment_request }}</v-card-text>
                  </v-card>
              </template>
            </v-data-table>
            </v-card>
        </v-container>
    `,
  data() {
    return {
      expand: false,
      headers: [
        {"text": "Add Index", "value": "add_index"},
        {"text": "r Preimage", "value": "r_preimage"},
        {"text": "r Hash", "value": "r_hash"},
        {"text": "Value", "value": "value"},
        {"text": "Settled", "value": "settled"},
        {"text": "Expiry", "value": "expiry"},
        {"text": "State", "value": "state"}
      ],
      pagination: {
        descending: true,
        rowsPerPage: 20,
        sortBy: "add_index",
      },
      invoices: [],
      chainBalance: null,
      channelBalance: null
    }
  },
  created() {
    this.init();
  },
  methods: {
    init: function () {
      if (!lightning.hasInit()) {
        Msgbus.$emit("error", "Please config lightning network first.");
        this.$router.push({name: "config"});
        return
      }
      Msgbus.$on("refresh", () => {
        this.refresh();
      });
      this.refresh();
    },
    fetchInvoices: function () {
      Msgbus.$emit("loading", true);
      lightning.invoice().then(json => {
        this.invoices = json.invoices;
        Msgbus.$emit("loading", false);
      }).catch(errorHandler)
    },
    fetchChainBalance: function () {
      lightning.chainBalance().then(json => this.chainBalance = json
      ).catch(errorHandler)
    },
    fetchChannelBalance: function () {
      lightning.channelBalance().then(json => this.channelBalance = json
      ).catch(errorHandler)
    },
    getInfo: function () {
      lightning.getinfo().catch(errorHandler);
    },
    refresh: function () {
      this.fetchChainBalance();
      this.fetchChannelBalance();
      this.fetchInvoices();
    }
  },
  computed: {},
  components: {
    "json-object-view": JsonObjViewComponent,
  }
});
