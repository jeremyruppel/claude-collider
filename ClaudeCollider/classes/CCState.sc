// CCState - Session state management for ClaudeCollider

CCState {
  var <cc;
  var <buses;

  *new { |cc|
    ^super.new.init(cc);
  }

  init { |argCC|
    cc = argCC;
    buses = Dictionary[];
  }

  bus { |name, numChannels=1, rate=\control|
    var bus = buses[name.asSymbol];
    if(bus.isNil) {
      bus = if(rate == \audio) {
        Bus.audio(cc.server, numChannels);
      } {
        Bus.control(cc.server, numChannels);
      };
      buses[name.asSymbol] = bus;
      currentEnvironment[name.asSymbol] = bus;
    };
    ^bus;
  }

  setBus { |name, value|
    var bus = buses[name.asSymbol];
    if(bus.notNil) {
      bus.set(value);
      ^true;
    };
    ^false;
  }

  getBus { |name|
    ^buses[name.asSymbol];
  }

  freeBus { |name|
    var bus = buses[name.asSymbol];
    if(bus.notNil) {
      bus.free;
      buses.removeAt(name.asSymbol);
      currentEnvironment.removeAt(name.asSymbol);
      ^true;
    };
    ^false;
  }

  clear {
    buses.do(_.free);
    buses.keysValuesDo { |name|
      currentEnvironment.removeAt(name);
    };
    buses.clear;
  }

  status {
    ^(
      buses: buses.keys.asArray
    );
  }
}
