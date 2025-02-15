package roundtrip;

import java.util.Optional;

import jynx.MainOption;
import jynx.MainOptionService;

public class MainRoundTrip implements MainOptionService {

    @Override
    public MainOption main() {
        return MainOption.ROUNDTRIP;
    }

    @Override
    public boolean call(Optional<String> optfname) {
        return RoundTrip.roundTrip(optfname);
    }
    
}
