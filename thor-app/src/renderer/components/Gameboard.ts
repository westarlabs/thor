import Vue from "vue";
import * as vm from "../sdk/vm";
import {ICanvasSYS} from "as2d/src/util/ICanvasSYS";
import * as loader from "assemblyscript/lib/loader";
import {GameGUI} from "../sdk/GameGUI";
import util from "../sdk/util";

let VueCountdown = require('@chenfengyuan/vue-countdown');

Vue.component(VueCountdown.name, VueCountdown);

interface ComponentData {
  game?: ICanvasSYS & loader.ASUtil & GameGUI | null;
  countDownStarted: boolean;
}

export default Vue.extend({
  template: `
        <v-responsive>
          <v-card v-if="gameInfo">
          <v-card-title>
          <span class="headline">{{gameInfo.base.gameName}}</span><v-spacer></v-spacer>
          <span>
            <countdown ref="countdown" v-bind:time="timeout*1000" :auto-start="false" @end="onCountDownEnd">
            <template slot-scope="props"> <v-icon>timer</v-icon> {{ props.minutes }} ： {{ props.seconds }} </template>
            </countdown>
          </span>
          </v-card-title>
          <v-responsive>
            <canvas id="as2d" width="450" height="450"/>
          </v-responsive>
          </v-card>
        </v-responsive>
  `,
  props: {
    role: {
      type: Number,
      default: 1
    },
    gameInfo: {
      type: Object
    },
    startTime: {
      type: Number,
      default: Date.now()
    },
    timeout: {
      type: Number,
      default: 60
    },
    playWithAI: {
      type: Boolean,
      default: false
    }
  },
  data(): ComponentData {
    return {
      game: null,
      countDownStarted: false
    }
  },
  created() {

  },
  methods: {
    startGame: function () {
      let engineBuffer = util.decodeHex(this.gameInfo.engineBytes);
      let guiBuffer = util.decodeHex(this.gameInfo.guiBytes);
      let self = this;
      vm.init(this.role, function (player, fullState, state) {
        if (player == self.role) {
          // @ts-ignore
          self.$refs.countdown.pause();
        } else {
          if (self.countDownStarted) {
            // @ts-ignore
            self.$refs.countdown.continue();
          } else {
            self.startCountDown();
          }
        }
        self.$emit("gameStateUpdate", {player: player, fullState: fullState, state: state});
      }, engineBuffer, guiBuffer, function (player: number) {
        // @ts-ignore
        self.$refs.countdown.abort();
        self.$emit("gameOver", player);
      }, function (error: string) {
        self.$emit("error", error);
      }, this.playWithAI).then(module => {
        this.game = module;
        this.game.startGame();
        if (this.role == 1) {
          this.startCountDown();
        }
        return module;
      });
    },
    startCountDown: function () {
      // @ts-ignore
      this.$refs.countdown.start();
      this.countDownStarted = true;
    },
    onCountDownEnd: function () {
      this.$emit("gameTimeout");
    },
    rivalStateUpdate: function (state: Int8Array) {
      this.game!.rivalUpdate(this.game!.newArray(state));
    },
    getState: function () {
      let pointer = this.game!.getState();
      let fullState = this.game!.getArray(Int8Array, pointer);
      return fullState;
    }
  },
  computed: {},
  components: {}
});
